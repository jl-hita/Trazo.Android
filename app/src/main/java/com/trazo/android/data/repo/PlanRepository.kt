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
import com.trazo.android.data.sync.SyncScheduler
import com.trazo.android.diagnostics.Diagnostics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** A value and whether it came from the short-lived local copy because the network failed. */
data class Fetched<T>(val value: T, val fromCache: Boolean = false)

sealed interface LogOutcome {
    /** The server stored the log; [result] holds the updated session and the adaptation report. */
    data class Sent(val result: LogResult) : LogOutcome

    /** No connection: the log is saved locally and will be sent when possible. */
    data object Queued : LogOutcome

    data class Rejected(val error: ApiError) : LogOutcome
}

class PlanRepository(
    private val api: PlansApi,
    private val cache: PlanCache,
    private val queue: PendingLogQueue,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clearDrafts: () -> Unit = {},
    private val sync: SyncScheduler = SyncScheduler.None,
    /** Puts the sets of a rejected log back in the workout screen, so it can be corrected. */
    private val restoreDraft: (PendingLog) -> Unit = {},
    private val deleteDraft: (String) -> Unit = {}
) {
    /** Cache and queue are JSON files: reading or writing them must never happen on the main thread. */
    private suspend fun <T> disk(block: () -> T): T = withContext(io) { block() }

    private val _week = MutableStateFlow<WeekView?>(null)

    /** The current week as last seen, shared by the plan, session and workout screens. */
    val week: StateFlow<WeekView?> = _week.asStateFlow()

    private val _pending = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pending.asStateFlow()

    private val _rejected = MutableStateFlow<List<PendingLog>>(emptyList())

    /** Logs the server refused. They stay on the phone until the user sends them again, corrects them or deletes them. */
    val rejected: StateFlow<List<PendingLog>> = _rejected.asStateFlow()

    /** One delivery at a time: the queue, and a log being sent from the workout screen. */
    private val flushLock = Mutex()

    private suspend fun publishQueue() {
        val entries = disk { queue.all() }
        publish(entries)
    }

    /** After the consent for health data is withdrawn: what is still queued loses it, and the cached copies (injured areas) are dropped. */
    suspend fun forgetHealthData() {
        disk {
            queue.all().forEach { queue.replace(it.withoutHealthData()) }
            cache.clear()
        }
        publishQueue()
    }

    private fun publish(entries: List<PendingLog>) {
        _pending.value = entries.count { !it.isRejected }
        _rejected.value = entries.filter { it.isRejected }
    }

    /**
     * Counts the logs an earlier run left in the queue (app killed, no connection) and hands them to the background sync.
     * Called at start-up off the main thread: the queue is encrypted, so reading it touches the Keystore.
     */
    suspend fun resumePendingSync() {
        publishQueue()
        if (_pending.value > 0) sync.schedule()
    }

    suspend fun options(): ApiResult<PlanOptions> = apiCall { api.options() }

    suspend fun createPlan(request: PlanProfileRequest): ApiResult<PlanView> {
        val result = apiCall { api.create(request) }
        if (result is ApiResult.Success) { _week.value = null; disk { cache.clear(); cache.savePlan(result.value) } }
        return result
    }

    /** Creates a plan written by the user; free for every account. */
    /** Rewrites the routines of the user's own plan from today on; the cached week is dropped so the next read is fresh. */
    suspend fun updateManualPlan(request: ManualPlanRequest): ApiResult<PlanView> {
        val result = apiCall { api.updateManual(request) }
        if (result is ApiResult.Success) { _week.value = null; disk { cache.clear(); cache.savePlan(result.value) } }
        return result
    }

    /** Rewrites a pending session of a generated plan. It can reach other sessions, so the week is read again. */
    suspend fun editSession(sessionId: String, request: SessionEditRequest): ApiResult<SessionView> {
        val result = apiCall { api.editSession(sessionId, request) }
        if (result is ApiResult.Success) { _week.value = null; carried = null; disk { cache.clear() }; currentWeek() }
        return result
    }

    /** Forgets the lasting changes: blocks generated from now on are the plan's own proposal again. */
    suspend fun clearAdjustments(): ApiResult<Unit> = apiCall { api.clearAdjustments() }

    suspend fun programs(): ApiResult<List<ProgramView>> = apiCall { api.programs() }

    suspend fun programSheet(id: String, request: PlanProfileRequest): ApiResult<ProgramSheetView> = apiCall { api.programSheet(id, request) }

    suspend fun createProgramPlan(id: String, request: PlanProfileRequest): ApiResult<PlanView> {
        val result = apiCall { api.createProgram(id, request) }
        if (result is ApiResult.Success) { _week.value = null; disk { cache.clear(); cache.savePlan(result.value) } }
        return result
    }

    suspend fun createManualPlan(request: ManualPlanRequest): ApiResult<PlanView> {
        val result = apiCall { api.createManual(request) }
        if (result is ApiResult.Success) { _week.value = null; disk { cache.clear(); cache.savePlan(result.value) } }
        return result
    }

    suspend fun currentPlan(): ApiResult<Fetched<PlanView>> {
        val result = apiCall { api.current() }
        return when {
            result is ApiResult.Success -> { disk { cache.savePlan(result.value) }; ApiResult.Success(Fetched(result.value)) }
            result is ApiResult.Failure && result.error.kind == ErrorKind.Network -> disk { cache.plan() }?.let { ApiResult.Success(Fetched(it, true)) } ?: result
            result is ApiResult.Failure -> { if (result.error.code == NO_ACTIVE_PLAN) disk { cache.clear() }; result }
            else -> result as ApiResult.Failure
        }
    }

    suspend fun currentWeek(): ApiResult<Fetched<WeekView>> {
        flushPending()
        val result = apiCall { api.week() }
        return when (result) {
            is ApiResult.Success -> { disk { cache.saveWeek(result.value) }; _week.value = result.value; ApiResult.Success(Fetched(result.value)) }
            is ApiResult.Failure -> {
                if (result.error.kind == ErrorKind.Network) {
                    val cached = _week.value ?: disk { cache.week() }
                    if (cached != null) { _week.value = cached; return ApiResult.Success(Fetched(cached, true)) }
                }
                if (result.error.code == NO_ACTIVE_PLAN) { _week.value = null; disk { cache.clear() } }
                result
            }
        }
    }

    suspend fun load(): ApiResult<PlanLoadView> = apiCall { api.load() }

    suspend fun block(): ApiResult<BlockView> = apiCall { api.block() }

    /** The session to do next, decided by the backend (program order, whatever its date), with its position and recovery advice. */
    suspend fun next(): ApiResult<NextSessionView> {
        val result = apiCall { api.next() }
        if (result is ApiResult.Success) carried = result.value.session
        return result
    }

    /**
     * The next session as last seen. In a calendar plan it can be one of an earlier week (a late workout still to log), which the
     * current week does not hold, so the session and workout screens can still open it.
     */
    private var carried: SessionView? = null
    suspend fun progress(): ApiResult<ProgressView> = apiCall { api.progress() }

    /** The blocks of the active plan, asked against done; it fails without a plan. */
    suspend fun blockSummaries(): ApiResult<BlockSummaries> = apiCall { api.blockSummaries() }

    suspend fun history(take: Int = 30): ApiResult<List<HistoryItem>> = apiCall { api.history(take) }

    /** What the engine adjusted in the plan and why, newest first. */
    suspend fun adaptations(wholePlan: Boolean = false, take: Int = 50): ApiResult<AdaptationHistoryView> =
        apiCall { api.adaptations(take, if (wholePlan) "plan" else "block") }

    /** The latest manual-review request of the active plan; success with null when none was made yet. */
    suspend fun reviewRequest(messages: Boolean = false): ApiResult<ReviewRequestView?> {
        val result = apiCall { api.reviewRequest(messages) }
        return when {
            result is ApiResult.Success -> ApiResult.Success(result.value)
            result is ApiResult.Failure && result.error.code == "no_review_request" -> ApiResult.Success(null)
            else -> result as ApiResult.Failure
        }
    }

    suspend fun requestReview(note: String?): ApiResult<ReviewRequestView> =
        apiCall { api.requestReview(ReviewRequestBody(note?.trim()?.takeIf { it.isNotEmpty() })) }

    /** [clientRequestId] must be the same on every attempt to send one message: the server then stores it once. */
    suspend fun sendReviewMessage(body: String, clientRequestId: String): ApiResult<ReviewRequestView> =
        apiCall { api.sendReviewMessage(ReviewMessageBody(body.trim(), clientRequestId)) }

    /** Replaces the muscle priorities; they shape the blocks generated from now on, so the cached plan is refreshed and the week is kept. */
    suspend fun updateMusclePriorities(priorities: Map<String, String>): ApiResult<PlanView> {
        val result = apiCall { api.updateMusclePriorities(UpdateMusclePrioritiesRequest(priorities)) }
        if (result is ApiResult.Success) disk { cache.savePlan(result.value) }
        return result
    }

    /** Replaces the injured areas; the backend swaps what they rule out in the pending sessions, so the cached week is dropped and the plan refreshed. */
    suspend fun updateLimitations(areas: List<String>): ApiResult<PlanView> {
        val result = apiCall { api.updateLimitations(UpdateLimitationsRequest(areas)) }
        if (result is ApiResult.Success) { _week.value = null; disk { cache.clear(); cache.savePlan(result.value) } }
        return result
    }

    /** Moves the pending program of a calendar plan to the current week; the cached week is dropped so the next read is fresh. */
    suspend fun reschedule(): ApiResult<RescheduleResult> {
        val result = apiCall { api.reschedule() }
        if (result is ApiResult.Success) { _week.value = null; disk { cache.clear() } }
        return result
    }

    /** Omits a pending session on purpose; the cached week is dropped so the next read shows it resolved. */
    suspend fun skip(sessionId: String, reason: String): ApiResult<SkipResult> {
        val result = apiCall { api.skip(sessionId, SkipSessionRequest(reason)) }
        if (result is ApiResult.Success) { _week.value = null; disk { cache.clear() } }
        return result
    }

    suspend fun archive(): ApiResult<Unit> {
        val result = apiCall { api.archive() }
        if (result is ApiResult.Success) { _week.value = null; disk { cache.clear() } }
        return result
    }

    suspend fun alternatives(sessionId: String, exerciseId: String): ApiResult<AlternativesResult> =
        apiCall { api.alternatives(sessionId, exerciseId) }

    suspend fun addExercise(sessionId: String, exerciseId: String): ApiResult<SessionView> {
        val result = apiCall { api.addExercise(sessionId, AddExerciseRequest(exerciseId)) }
        if (result is ApiResult.Success) replaceSession(result.value)
        return result
    }

    suspend fun swap(sessionId: String, exerciseId: String, replacementId: String, remainingBlock: Boolean): ApiResult<SessionView> {
        val result = apiCall { api.swap(sessionId, exerciseId, SwapRequest(replacementId, remainingBlock)) }
        if (result is ApiResult.Success) replaceSession(result.value)
        return result
    }

    /**
     * Sends the finished workout. The log is saved in the local queue BEFORE it is sent and leaves it only when the server
     * answers for good, so a process killed (or a screen closed) in the middle of the request loses nothing. On a connection
     * failure or a server error it stays queued for [flushPending]; other rejections are returned to the user.
     */
    suspend fun submitLog(sessionId: String, request: LogSessionRequest, now: Long = System.currentTimeMillis()): LogOutcome {
        // The moment of the workout and the identity of the request are fixed HERE, once: the first attempt, every retry and a
        // later send from the queue carry the same values, so a log delivered late is still dated when the workout happened.
        val entry = PendingLog(sessionId, stamp(sessionId, request, now), now)
        // A phone that cannot save it can still send it: the failure is reported and the request goes on.
        try { disk { queue.add(entry) } } catch (e: CancellationException) { throw e } catch (e: Exception) { Diagnostics.report(e) }
        var answered = false
        try {
            return flushLock.withLock {
                when (val result = apiCall { api.log(sessionId, entry.request) }) {
                    is ApiResult.Success -> {
                        answered = true
                        disk { queue.remove(entry) }
                        replaceSession(result.value.session)
                        LogOutcome.Sent(result.value)
                    }
                    is ApiResult.Failure -> when {
                        // The server answers a repeated log with success, so the session holds something else: as when it comes from the
                        // queue, the log is kept with the reason until the user retries or deletes it.
                        result.error.code == SESSION_ALREADY_LOGGED -> {
                            answered = true; disk { queue.replace(entry.rejected(result.error)) }; LogOutcome.Rejected(result.error)
                        }
                        // The server rejected it only because another change reached the plan at the same moment and stored nothing: the same log
                        // (same ClientRequestId) will be accepted later. Any other 409 stays a final answer.
                        // A session that expired is no verdict on the log: it waits for the user to sign in again.
                        result.error.retryable || result.error.code == PLAN_MODIFIED_CONCURRENTLY || result.error.kind == ErrorKind.Unauthorized -> {
                            disk { queue.add(entry) }; LogOutcome.Queued
                        }
                        // The user is looking at the error and keeps the draft: nothing to keep in the queue.
                        else -> { answered = true; disk { queue.remove(entry) }; LogOutcome.Rejected(result.error) }
                    }
                }
            }
        } finally {
            withContext(NonCancellable) {
                publishQueue()
                if (!answered && _pending.value > 0) sync.schedule()
            }
        }
    }

    /**
     * Serializes delivery so a foreground refresh and WorkManager cannot submit the same queue entries concurrently.
     * Logs are processed oldest-first: a transient network failure stops the pass (preserving order), while a permanent
     * validation/not-found/conflict response marks the entry as rejected, so it neither blocks the later workouts nor
     * disappears. Returns how many entries stopped waiting (delivered or rejected).
     */
    suspend fun flushPending(): Int = flushLock.withLock {
        var settled = 0
        for (item in disk { queue.all() }.filterNot { it.isRejected }.sortedBy { it.createdAtMillis }) {
            // Logs queued by an older version have no date: the moment they were queued is when the workout ended.
            val result = apiCall { api.log(item.sessionId, stamp(item.sessionId, item.request, item.createdAtMillis)) }
            if (result is ApiResult.Success) {
                replaceSession(result.value.session)
                disk { queue.remove(item) }
                settled++
                continue
            }
            val error = (result as ApiResult.Failure).error
            // Not final: the entry stays, untouched, and is sent again by a later pass.
            // Without a session none of the others can go either: they all wait for the next sign-in.
            if (error.kind == ErrorKind.Network || error.kind == ErrorKind.Unauthorized || error.code == PLAN_MODIFIED_CONCURRENTLY) break
            // Sending the same data again cannot fix these (the server answers a repeated log with success, so
            // "already logged" means the session holds something else): keep it and let the user decide.
            // Without Premium (402) a generated plan takes no logs: the user is told why and can retry after subscribing.
            if (error.kind == ErrorKind.Validation || error.kind == ErrorKind.NotFound || error.kind == ErrorKind.Conflict ||
                error.kind == ErrorKind.SubscriptionRequired
            ) {
                disk {
                    queue.replace(item.rejected(error))
                    try { restoreDraft(item) } catch (e: Exception) { Diagnostics.report(e) }
                }
                settled++
            }
        }
        publishQueue()
        settled
    }

    /** Sends a rejected log again, for instance after the cause was solved. */
    suspend fun retryRejected(entry: PendingLog): Int {
        flushLock.withLock {
            disk { queue.replace(entry.waiting()) }
            publishQueue()
        }
        sync.schedule()
        return flushPending()
    }

    /** The user gives the log up: it is deleted with the draft made from it. */
    suspend fun discardRejected(entry: PendingLog) = flushLock.withLock {
        disk { queue.remove(entry); deleteDraft(entry.sessionId) }
        publishQueue()
    }

    fun session(id: String): SessionView? = _week.value?.sessions?.firstOrNull { it.id == id } ?: carried?.takeIf { it.id == id }

    /** The session ended without the user asking: the copy of the plan goes, the unsent workouts and drafts stay. */
    fun forgetCachedPlan() {
        _week.value = null
        cache.clear()
        sync.cancel()
    }

    /** Wipes every local copy: used on sign-out, account deletion and when someone else signs in. */
    fun clearLocal() {
        _week.value = null
        cache.clear()
        queue.clear()
        sync.cancel()
        clearDrafts()
        publish(emptyList())
    }

    private suspend fun replaceSession(updated: SessionView) {
        if (carried?.id == updated.id) carried = updated
        val current = _week.value ?: return
        val week = current.copy(sessions = current.sessions.map { if (it.id == updated.id) updated else it })
        _week.value = week
        disk { cache.saveWeek(week) }
    }

    companion object {
        /**
         * Gives the request the time of the workout (when it was not chosen) and a request id derived from the session and the
         * moment it was stamped, so stamping the same log again always gives exactly the same values.
         */
        fun stamp(sessionId: String, request: LogSessionRequest, createdAtMillis: Long): LogSessionRequest {
            val performedAt = request.performedAt ?: java.time.Instant.ofEpochMilli(createdAtMillis).toString()
            val requestId = request.clientRequestId
                ?: java.util.UUID.nameUUIDFromBytes("$sessionId|$createdAtMillis".toByteArray()).toString()
            return if (performedAt == request.performedAt && requestId == request.clientRequestId) request
            else request.copy(performedAt = performedAt, clientRequestId = requestId)
        }

        const val NO_ACTIVE_PLAN = "no_active_plan"
        const val SESSION_ALREADY_LOGGED = "session_already_logged"
        const val PLAN_MODIFIED_CONCURRENTLY = "plan_modified_concurrently"
    }
}
