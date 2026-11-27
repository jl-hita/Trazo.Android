package com.trazo.android.ui.plan

import androidx.lifecycle.ViewModel
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.R
import com.trazo.android.diagnostics.launchGuarded
import androidx.lifecycle.viewModelScope
import com.trazo.android.data.model.*
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.ArchiveQuestion
import com.trazo.android.data.repo.ArchiveReason
import com.trazo.android.data.repo.PlanRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanUiState(
    val loading: Boolean = true,
    val needsOnboarding: Boolean = false,
    val plan: PlanView? = null,
    val week: WeekView? = null,
    /** The session to do next, decided by the backend. Null while unknown (offline): the week below still works. */
    val next: NextSessionView? = null,
    val fromCache: Boolean = false,
    /** Moving the program to this week is in progress. */
    val rescheduling: Boolean = false,
    /** Omitting a session is in progress. */
    val skipping: Boolean = false,
    /** Saving the muscle priorities is in progress. */
    val savingPriorities: Boolean = false,
    /** Saving the time per session is in progress. */
    val savingMinutes: Boolean = false,
    /** Saving the injured areas is in progress. */
    val savingLimitations: Boolean = false,
    /** Archiving the plan is in progress. */
    val archiving: Boolean = false,
    /** Waiting for the person to say why the plan is archived. */
    val archiveQuestion: ArchiveQuestion? = null,
    val error: ApiError? = null
)

/** The "Plan" tab: current week with its sessions and the macrocycle summary. */
class PlanViewModel(
    private val plans: PlanRepository,
    private val archiveQuestion: suspend () -> ArchiveQuestion? = { null }
) : ViewModel() {
    private val _state = MutableStateFlow(PlanUiState())
    val state: StateFlow<PlanUiState> = _state.asStateFlow()
    val pendingLogs: StateFlow<Int> = plans.pendingCount
    val rejectedLogs: StateFlow<List<com.trazo.android.data.local.PendingLog>> = plans.rejected

    init { load() }

    /** The plan on screen, so an archive and its reason never reach another one. */
    private var archivedPlanId: String? = null

    /** The plan is over: closes it (history is kept) so the onboarding can create the next one, asking why first when that applies. */
    fun startNextPlan() {
        if (_state.value.archiving || _state.value.archiveQuestion != null) return
        archivedPlanId = _state.value.week?.planId
        _state.update { it.copy(archiving = true) }
        viewModelScope.launchGuarded(R.string.feature_plan, onFailure = { _state.update { it.copy(archiving = false) } }) {
            val question = archiveQuestion()
            if (question == null) archive(null) else _state.update { it.copy(archiving = false, archiveQuestion = question) }
        }
    }

    /** The answer to the question; null is "prefer not to say". */
    fun archiveBecause(reason: ArchiveReason?) {
        if (_state.value.archiveQuestion == null) return
        _state.update { it.copy(archiving = true, archiveQuestion = null) }
        viewModelScope.launchGuarded(R.string.feature_plan, onFailure = { _state.update { it.copy(archiving = false) } }) { archive(reason) }
    }

    /** Closing the question leaves the plan as it was. */
    fun cancelArchive() { _state.update { it.copy(archiveQuestion = null) } }

    private suspend fun archive(reason: ArchiveReason?) {
        plans.archive(archivedPlanId, reason)
        _state.update { it.copy(archiving = false) }
        load()
    }

    /** Moves the program to this week (calendar plans that fell behind) and reloads the plan. */
    fun reschedule() {
        if (_state.value.rescheduling) return
        _state.update { it.copy(rescheduling = true) }
        viewModelScope.launchGuarded(R.string.feature_plan, onFailure = { _state.update { it.copy(rescheduling = false, error = ApiError(ErrorKind.Unknown)) } }) {
            when (val result = plans.reschedule()) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _state.update { it.copy(rescheduling = false, error = result.error) }
            }
        }
    }

    /** Replaces the muscle priorities of the plan. They apply to the blocks generated from now on, so the week on screen is left alone. */
    fun updateMusclePriorities(priorities: Map<String, String>) {
        if (_state.value.savingPriorities) return
        _state.update { it.copy(savingPriorities = true, error = null) }
        viewModelScope.launchGuarded(R.string.feature_plan, onFailure = { _state.update { it.copy(savingPriorities = false, error = ApiError(ErrorKind.Unknown)) } }) {
            when (val result = plans.updateMusclePriorities(priorities)) {
                is ApiResult.Success -> _state.update { it.copy(savingPriorities = false, plan = result.value) }
                is ApiResult.Failure -> _state.update { it.copy(savingPriorities = false, error = result.error) }
            }
        }
    }

    /** Changes the time per session of a generated plan; it applies from the next block on. */
    fun updateSessionMinutes(minutes: Int) {
        if (_state.value.savingMinutes) return
        _state.update { it.copy(savingMinutes = true, error = null) }
        viewModelScope.launchGuarded(R.string.feature_plan, onFailure = { _state.update { it.copy(savingMinutes = false, error = ApiError(ErrorKind.Unknown)) } }) {
            when (val result = plans.updateSessionMinutes(minutes)) {
                is ApiResult.Success -> _state.update { it.copy(savingMinutes = false, plan = result.value) }
                is ApiResult.Failure -> _state.update { it.copy(savingMinutes = false, error = result.error) }
            }
        }
    }

    /** Reports (or clears) an injury outside a session: the plan adapts its pending sessions, so the week is reloaded. */
    fun updateLimitations(areas: List<String>) {
        if (_state.value.savingLimitations) return
        _state.update { it.copy(savingLimitations = true, error = null) }
        viewModelScope.launchGuarded(R.string.feature_plan, onFailure = { _state.update { it.copy(savingLimitations = false, error = ApiError(ErrorKind.Unknown)) } }) {
            when (val result = plans.updateLimitations(areas)) {
                is ApiResult.Success -> { _state.update { it.copy(savingLimitations = false, plan = result.value) }; load() }
                is ApiResult.Failure -> _state.update { it.copy(savingLimitations = false, error = result.error) }
            }
        }
    }

    /** Omits a pending session on purpose (the backend decides what that means for the plan) and reloads. */
    fun skip(sessionId: String, reason: String) {
        if (_state.value.skipping) return
        _state.update { it.copy(skipping = true) }
        viewModelScope.launchGuarded(R.string.feature_plan, onFailure = { _state.update { it.copy(skipping = false, error = ApiError(ErrorKind.Unknown)) } }) {
            when (val result = plans.skip(sessionId, reason)) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> _state.update { it.copy(skipping = false, error = result.error) }
            }
        }
    }

    fun sendPending() { viewModelScope.launchGuarded(R.string.feature_plan) { if (plans.flushPending() > 0) load() } }

    fun retryRejected(log: com.trazo.android.data.local.PendingLog) { viewModelScope.launchGuarded(R.string.feature_plan) { if (plans.retryRejected(log) > 0) load() } }

    fun discardRejected(log: com.trazo.android.data.local.PendingLog) { viewModelScope.launchGuarded(R.string.feature_plan) { plans.discardRejected(log) } }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launchGuarded(R.string.feature_plan, onFailure = { _state.value = PlanUiState(loading = false, error = ApiError(ErrorKind.Unknown)) }) {
            // The plan summary does not depend on the week: ask for both at once instead of one after the other.
            val planRequest = async { plans.currentPlan() }
            when (val week = plans.currentWeek()) {
                is ApiResult.Failure -> {
                    val noPlan = week.error.code == PlanRepository.NO_ACTIVE_PLAN
                    _state.value = PlanUiState(loading = false, needsOnboarding = noPlan, error = week.error.takeUnless { noPlan })
                }
                is ApiResult.Success -> {
                    val plan = (planRequest.await() as? ApiResult.Success)?.value
                    _state.value = PlanUiState(
                        loading = false, plan = plan?.value, week = week.value.value,
                        // Asked after the week, not with it: both can generate the plan's next block and must not race for it.
                        next = (plans.next() as? ApiResult.Success)?.value,
                        fromCache = week.value.fromCache || plan?.fromCache == true
                    )
                }
            }
        }
    }
}

data class LoadUiState(val loading: Boolean = true, val load: PlanLoadView? = null, val error: ApiError? = null)

/** Weekly planned and completed volume of the whole plan, for the chart of the overview. */
class LoadViewModel(private val plans: PlanRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoadUiState())
    val state: StateFlow<LoadUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = LoadUiState()
        viewModelScope.launchGuarded(R.string.feature_overview, onFailure = { _state.value = LoadUiState(false, error = ApiError(ErrorKind.Unknown)) }) {
            _state.value = when (val result = plans.load()) {
                is ApiResult.Success -> LoadUiState(false, result.value)
                is ApiResult.Failure -> LoadUiState(false, error = result.error)
            }
        }
    }
}

data class AdaptationsUiState(val loading: Boolean = true, val history: AdaptationHistoryView? = null, val error: ApiError? = null, val wholePlan: Boolean = false)

/** "What TRAZO adjusted": the engine's decisions with their numbers, so the value of the automatic adaptation is visible from the first sessions. */
class AdaptationsViewModel(private val plans: PlanRepository) : ViewModel() {
    private val _state = MutableStateFlow(AdaptationsUiState())
    val state: StateFlow<AdaptationsUiState> = _state.asStateFlow()

    init { load() }

    fun load(wholePlan: Boolean = _state.value.wholePlan) {
        _state.value = AdaptationsUiState(wholePlan = wholePlan)
        viewModelScope.launchGuarded(R.string.feature_adaptations, onFailure = { _state.value = AdaptationsUiState(false, error = ApiError(ErrorKind.Unknown), wholePlan = wholePlan) }) {
            _state.value = when (val result = plans.adaptations(wholePlan)) {
                is ApiResult.Success -> AdaptationsUiState(false, result.value, wholePlan = wholePlan)
                is ApiResult.Failure -> AdaptationsUiState(false, error = result.error, wholePlan = wholePlan)
            }
        }
    }

    /** The block in progress is the default; the rest of the plan is one tap away. */
    fun showWholePlan(whole: Boolean) = load(whole)
}

data class HistoryUiState(val loading: Boolean = true, val items: List<HistoryItem> = emptyList(), val error: ApiError? = null)

class HistoryViewModel(private val plans: PlanRepository) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = HistoryUiState()
        viewModelScope.launchGuarded(R.string.feature_history, onFailure = { _state.value = HistoryUiState(false, error = ApiError(ErrorKind.Unknown)) }) {
            _state.value = when (val result = plans.history()) {
                is ApiResult.Success -> HistoryUiState(false, result.value)
                is ApiResult.Failure -> HistoryUiState(false, error = result.error)
            }
        }
    }
}
