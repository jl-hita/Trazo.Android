package com.trazo.android.data.repo

import com.trazo.android.data.api.PlansApi
import com.trazo.android.data.local.PendingLog
import com.trazo.android.data.local.PendingLogQueue
import com.trazo.android.data.local.PlanCache
import com.trazo.android.data.model.*
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.data.net.apiCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A value and whether it came from the short-lived local copy because the network failed. */
data class Fetched<T>(val value: T, val fromCache: Boolean = false)

sealed interface LogOutcome {
    /** The server stored the log; [result] holds the updated session and the adaptation report. */
    data class Sent(val result: LogResult) : LogOutcome

    /** The server already had this log (a previous attempt arrived but its answer was lost). */
    data object AlreadyLogged : LogOutcome

    /** No connection: the log is saved locally and will be sent when possible. */
    data object Queued : LogOutcome

    data class Rejected(val error: ApiError) : LogOutcome
}

class PlanRepository(
    private val api: PlansApi,
    private val cache: PlanCache,
    private val queue: PendingLogQueue,
    private val clearDrafts: () -> Unit = {}
) {
    private val _week = MutableStateFlow<WeekView?>(null)

    /** The current week as last seen, shared by the plan, session and workout screens. */
    val week: StateFlow<WeekView?> = _week.asStateFlow()

    private val _pending = MutableStateFlow(queue.all().size)
    val pendingCount: StateFlow<Int> = _pending.asStateFlow()

    private val flushLock = Mutex()

    suspend fun options(): ApiResult<PlanOptions> = guarded { apiCall { api.options() } }

    suspend fun createPlan(request: PlanProfileRequest): ApiResult<PlanView> {
        val result = guarded { apiCall { api.create(request) } }
        if (result is ApiResult.Success) { _week.value = null; cache.clear(); cache.savePlan(result.value) }
        return result
    }

    suspend fun currentPlan(): ApiResult<Fetched<PlanView>> {
        val result = guarded { apiCall { api.current() } }
        return when {
            result is ApiResult.Success -> { cache.savePlan(result.value); ApiResult.Success(Fetched(result.value)) }
            result is ApiResult.Failure && result.error.kind == ErrorKind.Network -> cache.plan()?.let { ApiResult.Success(Fetched(it, true)) } ?: result
            result is ApiResult.Failure -> { if (result.error.code == NO_ACTIVE_PLAN) cache.clear(); result }
            else -> result as ApiResult.Failure
        }
    }

    suspend fun currentWeek(): ApiResult<Fetched<WeekView>> {
        flushPending()
        val result = guarded { apiCall { api.week() } }
        return when (result) {
            is ApiResult.Success -> { cache.saveWeek(result.value); _week.value = result.value; ApiResult.Success(Fetched(result.value)) }
            is ApiResult.Failure -> {
                if (result.error.kind == ErrorKind.Network) {
                    val cached = _week.value ?: cache.week()
                    if (cached != null) { _week.value = cached; return ApiResult.Success(Fetched(cached, true)) }
                }
                if (result.error.code == NO_ACTIVE_PLAN) { _week.value = null; cache.clear() }
                result
            }
        }
    }

    suspend fun history(take: Int = 30): ApiResult<List<HistoryItem>> = guarded { apiCall { api.history(take) } }

    suspend fun archive(): ApiResult<Unit> {
        val result = guarded { apiCall { api.archive() } }
        if (result is ApiResult.Success) { _week.value = null; cache.clear() }
        return result
    }

    suspend fun alternatives(sessionId: String, exerciseId: String): ApiResult<AlternativesResult> =
        guarded { apiCall { api.alternatives(sessionId, exerciseId) } }

    suspend fun swap(sessionId: String, exerciseId: String, replacementId: String, remainingBlock: Boolean): ApiResult<SessionView> {
        val result = guarded { apiCall { api.swap(sessionId, exerciseId, SwapRequest(replacementId, remainingBlock)) } }
        if (result is ApiResult.Success) replaceSession(result.value)
        return result
    }

    /**
     * Sends the finished workout. On a connection failure or a server error the log is kept in a local queue (without
     * loss) and delivered later by [flushPending]; other rejections are returned to the user.
     */
    suspend fun submitLog(sessionId: String, request: LogSessionRequest, now: Long = System.currentTimeMillis()): LogOutcome {
        val result = guarded { apiCall { api.log(sessionId, request) } }
        return when (result) {
            is ApiResult.Success -> { replaceSession(result.value.session); LogOutcome.Sent(result.value) }
            is ApiResult.Failure -> when {
                result.error.code == SESSION_ALREADY_LOGGED -> LogOutcome.AlreadyLogged
                result.error.retryable -> { queue.add(PendingLog(sessionId, request, now)); _pending.value = queue.all().size; LogOutcome.Queued }
                else -> LogOutcome.Rejected(result.error)
            }
        }
    }

    /** Tries to deliver the queued logs, oldest first. Returns how many were removed from the queue. */
    suspend fun flushPending(): Int = flushLock.withLock {
        var removed = 0
        for (item in queue.all().sortedBy { it.createdAtMillis }) {
            val result = guarded { apiCall { api.log(item.sessionId, item.request) } }
            val done = when (result) {
                is ApiResult.Success -> true.also { replaceSession(result.value.session) }
                is ApiResult.Failure -> when {
                    result.error.code == SESSION_ALREADY_LOGGED -> true
                    // Retrying cannot fix these: drop the entry instead of blocking the queue forever.
                    result.error.kind == ErrorKind.Validation || result.error.kind == ErrorKind.NotFound || result.error.kind == ErrorKind.Conflict -> true
                    else -> false
                }
            }
            if (done) { queue.remove(item.sessionId); removed++ } else if (result is ApiResult.Failure && result.error.kind == ErrorKind.Network) break
        }
        _pending.value = queue.all().size
        removed
    }

    fun session(id: String): SessionView? = _week.value?.sessions?.firstOrNull { it.id == id }

    /** Wipes every local copy: used on sign-out and when the account loses access. */
    fun clearLocal() {
        _week.value = null
        cache.clear()
        queue.clear()
        clearDrafts()
        _pending.value = 0
    }

    private fun replaceSession(updated: SessionView) {
        val current = _week.value ?: return
        val week = current.copy(sessions = current.sessions.map { if (it.id == updated.id) updated else it })
        _week.value = week
        cache.saveWeek(week)
    }

    /** A 402 means the account has no access: nothing of the plan may stay on the device. */
    private suspend fun <T> guarded(call: suspend () -> ApiResult<T>): ApiResult<T> {
        val result = call()
        if (result is ApiResult.Failure && result.error.kind == ErrorKind.SubscriptionRequired) { _week.value = null; cache.clear() }
        return result
    }

    companion object {
        const val NO_ACTIVE_PLAN = "no_active_plan"
        const val SESSION_ALREADY_LOGGED = "session_already_logged"
    }
}
