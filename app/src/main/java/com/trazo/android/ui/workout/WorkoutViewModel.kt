package com.trazo.android.ui.workout

import androidx.lifecycle.ViewModel
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.R
import com.trazo.android.diagnostics.launchGuarded
import androidx.lifecycle.viewModelScope
import com.trazo.android.data.local.DraftSet
import com.trazo.android.data.local.WorkoutDraftStore
import com.trazo.android.data.model.*
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.LogOutcome
import com.trazo.android.data.repo.PlanRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val sessionId: String,
    io: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    // One writer thread keeps the drafts in order; typing never waits for the disk.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val draftWriter = io.limitedParallelism(1)
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

    fun stepWeight(exerciseId: String, index: Int, up: Boolean) {
        val ex = _state.value.entries.firstOrNull { it.exercise.exerciseId == exerciseId }?.exercise ?: return
        updateSet(exerciseId, index) { it.copy(weight = stepWeight(it.weight, up, proposedLoad(ex))) }
    }

    fun stepReps(exerciseId: String, index: Int, up: Boolean) {
        val ex = _state.value.entries.firstOrNull { it.exercise.exerciseId == exerciseId }?.exercise ?: return
        updateSet(exerciseId, index) { it.copy(reps = stepReps(it.reps, up, ex.minRepetitions)) }
    }

    /** Marks a set done (it needs repetitions, as the API requires) and starts the rest timer of that exercise. */
    fun toggleDone(exerciseId: String, index: Int) {
        val entry = _state.value.entries.firstOrNull { it.exercise.exerciseId == exerciseId } ?: return
        val set = entry.sets.getOrNull(index) ?: return
        if (!set.done && parseReps(set.reps) == null) return
        updateSet(exerciseId, index) { it.copy(done = !it.done) }
        // The next set that is still open starts from what was just done: the usual case is the same load and reps.
        if (!set.done) {
            val next = entry.sets.getOrNull(index + 1)
            if (next != null && !next.done) updateSet(exerciseId, index + 1) { it.copy(weight = set.weight.ifBlank { it.weight }, reps = set.reps) }
            startRest(entry.exercise.restSeconds)
        }
    }

    fun startRest(seconds: Int) {
        timer?.cancel()
        if (seconds <= 0) { _state.update { it.copy(rest = null) }; return }
        _state.update { it.copy(rest = RestTimer(seconds, seconds)) }
        timer = viewModelScope.launchGuarded(R.string.feature_workout) {
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
        viewModelScope.launchGuarded(R.string.feature_workout, onFailure = { _state.update { it.copy(swapError = ApiError(ErrorKind.Unknown)) } }) {
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
        viewModelScope.launchGuarded(R.string.feature_workout, onFailure = { _state.update { it.copy(finish = FinishState.Failed(ApiError(ErrorKind.Unknown))) } }) {
            val request = buildLogRequest(s.entries, form)
            val outcome = plans.submitLog(session.id, request)
            val next = when (outcome) {
                is LogOutcome.Sent -> FinishState.Done(outcome.result, queued = false)
                LogOutcome.AlreadyLogged -> FinishState.Done(null, queued = false)
                LogOutcome.Queued -> FinishState.Done(null, queued = true)
                is LogOutcome.Rejected -> FinishState.Failed(outcome.error)
            }
            if (next is FinishState.Done) { withContext(draftWriter) { drafts.delete(session.id) }; timer?.cancel() }
            _state.update { it.copy(finish = next, rest = if (next is FinishState.Done) null else it.rest) }
        }
    }

    fun dismissFinishError() = _state.update { it.copy(finish = FinishState.Idle) }

    private fun saveDraft() {
        val sets = _state.value.entries.flatMap { e ->
            e.sets.mapIndexed { i, set -> DraftSet(e.exercise.exerciseId, i + 1, set.weight, set.reps, set.rir, set.done) }
        }
        // Not tied to viewModelScope: leaving the screen right after typing must still persist the last edit.
        CoroutineScope(draftWriter).launchGuarded(R.string.feature_workout) { drafts.save(sessionId, sets) }
    }

    override fun onCleared() { timer?.cancel() }

    companion object {
        fun freshEntry(ex: ExerciseView) = ExerciseEntry(
            ex, List(ex.sets) { i -> SetEntry(weight = proposedLoad(ex)?.let(::formatKg).orEmpty(), reps = proposedReps(ex, i + 1)?.toString().orEmpty()) }
        )

        /** Repetitions to start from: the same set of last time, else its last set, else the bottom of the range. */
        fun proposedReps(ex: ExerciseView, setNumber: Int): Int? {
            val last = ex.lastPerformance?.sets?.filter { it.repetitions != null }.orEmpty()
            return (last.firstOrNull { it.setNumber == setNumber } ?: last.lastOrNull())?.repetitions
                ?: ex.minRepetitions.takeIf { it > 0 }
        }

        /** One step of the weight buttons: 1 kg up to 20 kg, 2.5 kg above. Empty starts from [start]. */
        fun stepWeight(text: String, up: Boolean, start: Double?): String {
            val current = parseWeight(text) ?: return formatKg(if (up) start ?: 1.0 else 0.0)
            val step = if (current < 20.0 || (!up && current <= 20.0)) 1.0 else 2.5
            return formatKg((if (up) current + step else current - step).coerceIn(0.0, 1000.0))
        }

        fun stepReps(text: String, up: Boolean, start: Int): String {
            val current = parseReps(text) ?: return if (up) start.toString() else "0"
            return (if (up) current + 1 else current - 1).coerceIn(0, 100).toString()
        }

        /**
         * The load to start with: the engine's target when it has one, else the proposal made from the last time (repeat the
         * heaviest load, or one increment more when every set reached the top of the range), else the last load used.
         */
        fun proposedLoad(ex: ExerciseView): Double? =
            ex.targetLoadKg ?: ex.lastPerformance?.let { last ->
                last.suggestedLoadKg ?: last.sets.mapNotNull { it.weightKg }.maxOrNull()
            }

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
                        else -> LogSetRequest(e.exercise.exerciseId, i + 1, null, null, null, false, skipReasonFor[reason])
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
