package com.trazo.android.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.android.R
import com.trazo.android.data.local.WorkoutDraftStore
import com.trazo.android.data.model.SessionView
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.data.repo.ExerciseRepository
import com.trazo.android.data.repo.PlanRepository
import com.trazo.android.diagnostics.launchGuarded
import com.trazo.android.ui.workout.AlternativesState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class SessionUiState(
    /** A session the cached week does not hold (a past one): found in the block, with what was logged in it. */
    val found: SessionView? = null,
    val searching: Boolean = false,
    /** The session as it is after a swap made from this screen. */
    val swapped: SessionView? = null,
    val alternatives: AlternativesState? = null,
    val swapError: ApiError? = null,
    /** A generated plan: its pending sessions can be edited. */
    val generated: Boolean = false,
    /** Something of this session was noted and not sent yet: the workout can be taken up where it was left. */
    val started: Boolean = false
)

/** What the session screen does besides showing the prescription: finds past sessions and swaps an exercise before the workout starts. */
class SessionViewModel(
    private val plans: PlanRepository,
    val catalog: ExerciseRepository,
    private val sessionId: String,
    private val drafts: WorkoutDraftStore? = null,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    val queuedSessions: StateFlow<Set<String>> = plans.queuedSessions

    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    init {
        if (plans.session(sessionId) == null) findInBlock()
        viewModelScope.launchGuarded(R.string.feature_workout, onFailure = { }) {
            val plan = (plans.currentPlan() as? ApiResult.Success)?.value?.value
            _state.update { it.copy(generated = plan?.origin == "Generated") }
        }
    }

    /** Read every time the screen is shown: the user may be coming back from the workout. */
    fun checkDraft() {
        val store = drafts ?: return
        viewModelScope.launchGuarded(R.string.feature_workout, onFailure = { }) {
            val started = withContext(io) {
                store.load(sessionId)?.let { draft ->
                    draft.sets.any { it.done || it.reps.isNotBlank() } || draft.muscles != null || draft.discomfort != null
                } == true
            }
            _state.update { it.copy(started = started) }
        }
    }

    /** Before leaving to edit: what a swap left on screen would hide what the edit changes. */
    fun forgetSwapped() = _state.update { it.copy(swapped = null) }

    private fun findInBlock() {
        _state.update { it.copy(searching = true) }
        viewModelScope.launchGuarded(R.string.feature_workout, onFailure = { _state.update { it.copy(searching = false) } }) {
            val found = (plans.block() as? ApiResult.Success)?.value?.weeks?.flatMap { it.sessions }?.firstOrNull { it.id == sessionId }
            _state.update { it.copy(found = found, searching = false) }
        }
    }

    fun showAlternatives(exerciseId: String) {
        _state.update { it.copy(alternatives = AlternativesState(exerciseId), swapError = null) }
        viewModelScope.launchGuarded(R.string.feature_workout, onFailure = { _state.update { it.copy(alternatives = it.alternatives?.copy(loading = false, error = ApiError(ErrorKind.Unknown))) } }) {
            val next = when (val r = plans.alternatives(sessionId, exerciseId)) {
                is ApiResult.Success -> AlternativesState(exerciseId, false, r.value.alternatives, r.value.message)
                is ApiResult.Failure -> AlternativesState(exerciseId, false, error = r.error)
            }
            _state.update { if (it.alternatives?.exerciseId == exerciseId) it.copy(alternatives = next) else it }
        }
    }

    fun dismissAlternatives() = _state.update { it.copy(alternatives = null, swapError = null) }

    fun swap(exerciseId: String, replacementId: String, remainingBlock: Boolean) {
        if (_state.value.alternatives?.swapping == true) return
        _state.update { it.copy(alternatives = it.alternatives?.copy(swapping = true), swapError = null) }
        viewModelScope.launchGuarded(R.string.feature_workout, onFailure = { _state.update { it.copy(alternatives = it.alternatives?.copy(swapping = false), swapError = ApiError(ErrorKind.Unknown)) } }) {
            when (val r = plans.swap(sessionId, exerciseId, replacementId, remainingBlock)) {
                is ApiResult.Success -> _state.update { it.copy(swapped = r.value, alternatives = null, swapError = null) }
                is ApiResult.Failure -> _state.update { it.copy(alternatives = it.alternatives?.copy(swapping = false), swapError = r.error) }
            }
        }
    }
}
