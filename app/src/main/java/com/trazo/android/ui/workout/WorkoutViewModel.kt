package com.trazo.android.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.android.data.local.DraftSet
import com.trazo.android.data.local.WorkoutDraftStore
import com.trazo.android.data.model.*
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.LogOutcome
import com.trazo.android.data.repo.PlanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetEntry(val weight: String = "", val reps: String = "", val rir: String = "", val done: Boolean = false)

data class ExerciseEntry(val exercise: ExerciseView, val sets: List<SetEntry>)

data class RestTimer(val totalSeconds: Int, val remainingSeconds: Int)

data class AlternativesState(
    val exerciseId: String,
    val loading: Boolean = true,
    val items: List<AlternativeView> = emptyList(),
    val message: String? = null,
    val error: ApiError? = null
)

sealed interface FinishState {
    data object Idle : FinishState
    data object Sending : FinishState
    data class Done(val result: LogResult?, val queued: Boolean) : FinishState
    data class Failed(val error: ApiError) : FinishState
}

data class WorkoutState(
    val session: SessionView? = null,
    val entries: List<ExerciseEntry> = emptyList(),
    val rest: RestTimer? = null,
    val alternatives: AlternativesState? = null,
    val swapError: ApiError? = null,
    val finish: FinishState = FinishState.Idle
) {
    val allSets: Int get() = entries.sumOf { it.sets.size }
    val doneSets: Int get() = entries.sumOf { e -> e.sets.count { it.done } }
}

class FinishForm(
    val recovery: String = "Unknown",
    val sessionRpe: Double? = null,
    val pain: Boolean = false,
    val notes: String = "",
    val reason: String = "None"
)

/** Records the real sets of one session, runs the rest timer and sends the log (or queues it when offline). */
class WorkoutViewModel(
    private val plans: PlanRepository,
    private val drafts: WorkoutDraftStore,
    private val sessionId: String
) : ViewModel() {
    private val _state = MutableStateFlow(WorkoutState())
    val state: StateFlow<WorkoutState> = _state.asStateFlow()
    private var timer: Job? = null

    init {
        val session = plans.session(sessionId)
        if (session != null) _state.value = WorkoutState(session, buildEntries(session, drafts.load(sessionId)?.sets.orEmpty()))
    }

    fun updateSet(exerciseId: String, index: Int, transform: (SetEntry) -> SetEntry) {
        _state.update { s ->
            s.copy(entries = s.entries.map { e ->
                if (e.exercise.exerciseId != exerciseId) e else e.copy(sets = e.sets.mapIndexed { i, set -> if (i == index) transform(set) else set })
            })
        }
        saveDraft()
    }

    /** Marks a set done (it needs repetitions, as the API requires) and starts the rest timer of that exercise. */
    fun toggleDone(exerciseId: String, index: Int) {
        val entry = _state.value.entries.firstOrNull { it.exercise.exerciseId == exerciseId } ?: return
        val set = entry.sets.getOrNull(index) ?: return
        if (!set.done && parseReps(set.reps) == null) return
        updateSet(exerciseId, index) { it.copy(done = !it.done) }
        if (!set.done) startRest(entry.exercise.restSeconds)
    }

    fun startRest(seconds: Int) {
        timer?.cancel()
        if (seconds <= 0) { _state.update { it.copy(rest = null) }; return }
        _state.update { it.copy(rest = RestTimer(seconds, seconds)) }
        timer = viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                delay(1000)
                left--
                _state.update { it.copy(rest = it.rest?.copy(remainingSeconds = left)) }
            }
        }
    }

    fun adjustRest(deltaSeconds: Int) {
        val rest = _state.value.rest ?: return
        startRest((rest.remainingSeconds + deltaSeconds).coerceAtLeast(0).coerceAtMost(900))
    }

    fun skipRest() { timer?.cancel(); _state.update { it.copy(rest = null) } }

    fun showAlternatives(exerciseId: String) {
        _state.update { it.copy(alternatives = AlternativesState(exerciseId), swapError = null) }
        viewModelScope.launch {
            val next = when (val r = plans.alternatives(sessionId, exerciseId)) {
                is ApiResult.Success -> AlternativesState(exerciseId, false, r.value.alternatives, r.value.message)
                is ApiResult.Failure -> AlternativesState(exerciseId, false, error = r.error)
            }
            _state.update { if (it.alternatives?.exerciseId == exerciseId) it.copy(alternatives = next) else it }
        }
    }

    fun dismissAlternatives() = _state.update { it.copy(alternatives = null, swapError = null) }

    fun swap(exerciseId: String, replacementId: String, remainingBlock: Boolean) {
        viewModelScope.launch {
            when (val r = plans.swap(sessionId, exerciseId, replacementId, remainingBlock)) {
                is ApiResult.Success -> _state.update { s ->
                    // Keep what was typed for the exercises that did not change; the swapped one starts fresh.
                    val keep = s.entries.associateBy { it.exercise.exerciseId }
                    s.copy(
                        session = r.value, alternatives = null, swapError = null,
                        entries = r.value.exercises.map { ex -> keep[ex.exerciseId]?.copy(exercise = ex) ?: freshEntry(ex) }
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(swapError = r.error) }
            }
            saveDraft()
        }
    }

    fun finish(form: FinishForm) {
        val s = _state.value
        val session = s.session ?: return
        if (s.finish is FinishState.Sending) return
        _state.update { it.copy(finish = FinishState.Sending) }
        viewModelScope.launch {
            val request = buildLogRequest(s.entries, form)
            val outcome = plans.submitLog(session.id, request)
            val next = when (outcome) {
                is LogOutcome.Sent -> FinishState.Done(outcome.result, queued = false)
                LogOutcome.AlreadyLogged -> FinishState.Done(null, queued = false)
                LogOutcome.Queued -> FinishState.Done(null, queued = true)
                is LogOutcome.Rejected -> FinishState.Failed(outcome.error)
            }
            if (next is FinishState.Done) { drafts.delete(session.id); timer?.cancel() }
            _state.update { it.copy(finish = next, rest = if (next is FinishState.Done) null else it.rest) }
        }
    }

    fun dismissFinishError() = _state.update { it.copy(finish = FinishState.Idle) }

    private fun saveDraft() {
        val entries = _state.value.entries
        drafts.save(sessionId, entries.flatMap { e ->
            e.sets.mapIndexed { i, set -> DraftSet(e.exercise.exerciseId, i + 1, set.weight, set.reps, set.rir, set.done) }
        })
    }

    override fun onCleared() { timer?.cancel() }

    companion object {
        fun freshEntry(ex: ExerciseView) = ExerciseEntry(
            ex, List(ex.sets) { SetEntry(weight = ex.targetLoadKg?.let(::formatKg).orEmpty()) }
        )

        fun buildEntries(session: SessionView, draft: List<DraftSet>): List<ExerciseEntry> = session.exercises.map { ex ->
            val saved = draft.filter { it.exerciseId == ex.exerciseId }.associateBy { it.setNumber }
            val base = freshEntry(ex)
            base.copy(sets = base.sets.mapIndexed { i, set ->
                saved[i + 1]?.let { SetEntry(it.weight, it.reps, it.rir, it.done) } ?: set
            })
        }

        fun formatKg(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

        /** Accepts a comma or a dot as decimal separator; returns null for empty or out-of-range input. */
        fun parseWeight(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..1000.0 }
        fun parseReps(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 0..100 }
        fun parseRir(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..10.0 }

        private val skipReasonFor = mapOf(
            "Time" to "time", "Fatigue" to "fatigue", "Equipment" to "equipment",
            "Motivation" to "motivation", "Illness" to "illness", "Other" to "other"
        )

        fun buildLogRequest(entries: List<ExerciseEntry>, form: FinishForm): LogSessionRequest {
            val total = entries.sumOf { it.sets.size }
            val done = entries.sumOf { e -> e.sets.count { it.done } }
            val outcome = when {
                done == 0 -> "NotCompleted"
                done == total -> "Completed"
                else -> "Partial"
            }
            val reason = if (outcome == "Completed") "None" else form.reason
            val sets = entries.flatMap { e ->
                e.sets.mapIndexedNotNull { i, set ->
                    val reps = parseReps(set.reps)
                    val weight = parseWeight(set.weight)
                    val rir = parseRir(set.rir)
                    when {
                        set.done && reps != null -> LogSetRequest(e.exercise.exerciseId, i + 1, weight, reps, rir, true)
                        // A set left undone is reported as skipped, which is what lets the engine see fatigue or lack of time.
                        else -> LogSetRequest(e.exercise.exerciseId, i + 1, weight, reps, rir, false, skipReasonFor[reason])
                    }
                }
            }
            return LogSessionRequest(
                outcome = outcome,
                nonCompletionReason = reason,
                recovery = form.recovery,
                sessionRpe = form.sessionRpe,
                painReported = form.pain,
                notes = form.notes.trim().take(1000).ifEmpty { null },
                sets = sets
            )
        }
    }
}
