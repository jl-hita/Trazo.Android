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
import com.trazo.android.data.repo.ExerciseFilters
import com.trazo.android.data.repo.ExerciseRepository
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

data class SetEntry(
    val weight: String = "", val reps: String = "", val rir: String = "", val done: Boolean = false,
    /** What followed this set in a technique, in order; the last one may be the segment still to do. */
    val segments: List<SetEntry> = emptyList()
)

data class ExerciseEntry(val exercise: ExerciseView, val sets: List<SetEntry>)

data class RestTimer(val totalSeconds: Int, val remainingSeconds: Int)

data class AlternativesState(
    val exerciseId: String,
    val loading: Boolean = true,
    val items: List<AlternativeView> = emptyList(),
    val message: String? = null,
    val error: ApiError? = null
)

data class AddExerciseState(
    val query: String = "",
    val results: List<ExerciseListItem> = emptyList(),
    val searching: Boolean = false,
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
    /** The picker that adds an exercise to this session only. */
    val adding: AddExerciseState? = null,
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
    val reason: String = "None",
    val feedback: Map<String, ExerciseFeedbackRequest> = emptyMap(),
    /** The day the workout happened when it was not today (a late registration); null means now. */
    val performedOn: java.time.LocalDate? = null
)

/** Records the real sets of one session, runs the rest timer and sends the log (or queues it when offline). */
class WorkoutViewModel(
    private val plans: PlanRepository,
    private val drafts: WorkoutDraftStore,
    private val sessionId: String,
    io: CoroutineDispatcher = Dispatchers.IO,
    private val exercises: ExerciseRepository? = null,
    /** Alerts the user when the rest ends while the app is not in view. */
    private val alarms: com.trazo.android.rest.RestAlarmScheduler = com.trazo.android.rest.RestAlarmScheduler.None,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    // One writer thread keeps the drafts in order; typing never waits for the disk.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val draftWriter = io.limitedParallelism(1)
    private val _state = MutableStateFlow(WorkoutState())
    val state: StateFlow<WorkoutState> = _state.asStateFlow()
    private var timer: Job? = null

    /** Where the exercise explanations come from (null in tests that do not need them). */
    val catalog: ExerciseRepository? get() = exercises

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
        if (set.done) return
        if (carried(entry, index) != null) continueTechnique(exerciseId, index) else restAfter(exerciseId)
    }

    fun updateSegment(exerciseId: String, index: Int, segment: Int, transform: (SetEntry) -> SetEntry) =
        updateSet(exerciseId, index) { set -> set.copy(segments = set.segments.mapIndexed { i, s -> if (i == segment) transform(s) else s }) }

    /** Marks a segment done and either offers the next one after its short rest or starts the normal rest. */
    fun toggleSegment(exerciseId: String, index: Int, segment: Int) {
        val set = entryOf(exerciseId)?.sets?.getOrNull(index) ?: return
        val target = set.segments.getOrNull(segment) ?: return
        if (!target.done && parseReps(target.reps) == null) return
        updateSegment(exerciseId, index, segment) { it.copy(done = !it.done) }
        if (!target.done) continueTechnique(exerciseId, index)
    }

    /** Offers one more segment even if the rule of the technique says it is over: the user decides. */
    fun addSegment(exerciseId: String, index: Int) {
        val entry = entryOf(exerciseId) ?: return
        val technique = carried(entry, index) ?: return
        val set = entry.sets.getOrNull(index) ?: return
        if (set.segments.size >= technique.maxSegments || set.segments.any { !it.done }) return
        updateSet(exerciseId, index) { it.copy(segments = it.segments + pendingSegment(technique, it)) }
    }

    /** The user stops here: the segment not done is dropped and the normal rest starts. */
    fun finishSet(exerciseId: String, index: Int) {
        updateSet(exerciseId, index) { it.copy(segments = it.segments.filter { s -> s.done }) }
        restAfter(exerciseId)
    }

    private fun entryOf(exerciseId: String) = _state.value.entries.firstOrNull { it.exercise.exerciseId == exerciseId }

    private fun continueTechnique(exerciseId: String, index: Int) {
        val entry = entryOf(exerciseId) ?: return
        val technique = carried(entry, index) ?: return
        val set = entry.sets.getOrNull(index) ?: return
        val pending = set.segments.any { !it.done }
        val another = pending || Techniques.wantsAnother(
            technique, parseReps(entry.sets.first().reps), parseReps(set.reps), set.segments.mapNotNull { parseReps(it.reps) }
        )
        if (!another) return restAfter(exerciseId)
        if (!pending) updateSet(exerciseId, index) { it.copy(segments = it.segments + pendingSegment(technique, it)) }
        startRest(technique.restSeconds)
    }

    /**
     * The rest after a finished set. Inside a superset the next exercise follows at once (or after the short rest the plan sets
     * between them) and the long rest comes after the last one of the round.
     */
    private fun restAfter(exerciseId: String) {
        val exercise = entryOf(exerciseId)?.exercise ?: return
        val superset = exercise.supersetGroup?.let { group -> _state.value.session?.supersets?.firstOrNull { it.group == group } }
        if (isLastOfGroup(_state.value.entries, exerciseId)) startRest(superset?.restAfterRoundSeconds ?: exercise.restSeconds)
        else if (superset != null) startRest(superset.restBetweenSeconds)
    }

    fun startRest(seconds: Int) {
        timer?.cancel()
        if (seconds <= 0) { alarms.cancel(); _state.update { it.copy(rest = null) }; return }
        _state.update { it.copy(rest = RestTimer(seconds, seconds)) }
        alarms.schedule(clock() + seconds * 1000L)
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

    fun skipRest() { timer?.cancel(); alarms.cancel(); _state.update { it.copy(rest = null) } }

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

    fun openAddExercise() {
        if (exercises == null) return
        _state.update { it.copy(adding = AddExerciseState()) }
        searchToAdd("")
    }

    fun closeAddExercise() = _state.update { it.copy(adding = null) }

    fun searchToAdd(query: String) {
        val repo = exercises ?: return
        _state.update { it.copy(adding = it.adding?.copy(query = query, searching = true, error = null)) }
        viewModelScope.launchGuarded(R.string.feature_workout, onFailure = { _state.update { s -> s.copy(adding = s.adding?.copy(results = emptyList(), searching = false)) } }) {
            val result = repo.search(ExerciseFilters(query = query), page = 1, pageSize = 20)
            _state.update { s ->
                val adding = s.adding
                // A newer query may have been typed meanwhile: ignore the stale answer.
                if (adding == null || adding.query != query) s
                else s.copy(adding = when (result) {
                    is ApiResult.Success -> adding.copy(results = result.value.items, searching = false)
                    is ApiResult.Failure -> adding.copy(results = emptyList(), searching = false, error = result.error)
                })
            }
        }
    }

    /** Adds the exercise to this session only (the server never repeats it in other sessions); what was typed is kept. */
    fun addExercise(item: ExerciseListItem) {
        viewModelScope.launchGuarded(R.string.feature_workout, onFailure = { _state.update { it.copy(adding = it.adding?.copy(error = ApiError(ErrorKind.Unknown))) } }) {
            when (val r = plans.addExercise(sessionId, item.id)) {
                is ApiResult.Success -> {
                    _state.update { s ->
                        val keep = s.entries.associateBy { it.exercise.exerciseId }
                        s.copy(
                            session = r.value, adding = null,
                            entries = r.value.exercises.map { ex -> keep[ex.exerciseId]?.copy(exercise = ex) ?: freshEntry(ex) }
                        )
                    }
                    saveDraft()
                }
                is ApiResult.Failure -> _state.update { it.copy(adding = it.adding?.copy(error = r.error)) }
            }
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

    /**
     * Submits a snapshot of the current entries. A queued offline result counts as safely handed off because the repository
     * has persisted it; only then is the editable draft deleted. A rejected request keeps the draft so the user can correct
     * the data and retry. The Sending guard prevents double taps from creating competing submissions.
     */
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
                LogOutcome.Queued -> FinishState.Done(null, queued = true)
                is LogOutcome.Rejected -> FinishState.Failed(outcome.error)
            }
            if (next is FinishState.Done) { withContext(draftWriter) { drafts.delete(session.id) }; timer?.cancel(); alarms.cancel() }
            _state.update { it.copy(finish = next, rest = if (next is FinishState.Done) null else it.rest) }
        }
    }

    fun dismissFinishError() = _state.update { it.copy(finish = FinishState.Idle) }

    private fun saveDraft() {
        val sets = _state.value.entries.flatMap { e ->
            e.sets.flatMapIndexed { i, set ->
                listOf(DraftSet(e.exercise.exerciseId, i + 1, set.weight, set.reps, set.rir, set.done)) +
                    set.segments.mapIndexed { k, s -> DraftSet(e.exercise.exerciseId, i + 1, s.weight, s.reps, s.rir, s.done, k + 1) }
            }
        }
        // Not tied to viewModelScope: leaving the screen right after typing must still persist the last edit.
        CoroutineScope(draftWriter).launchGuarded(R.string.feature_workout) { drafts.save(sessionId, sets) }
    }

    override fun onCleared() { timer?.cancel(); alarms.cancel() }

    companion object {
        /** False for an exercise that is followed by another one of the same superset. */
        fun isLastOfGroup(entries: List<ExerciseEntry>, exerciseId: String): Boolean {
            val i = entries.indexOfFirst { it.exercise.exerciseId == exerciseId }
            if (i < 0) return true
            val group = entries[i].exercise.supersetGroup ?: return true
            return entries.getOrNull(i + 1)?.exercise?.supersetGroup != group
        }

        /** The technique of the set at [index], or null when it is a straight set. */
        fun carried(entry: ExerciseEntry, index: Int): TechniqueView? =
            entry.exercise.technique?.takeIf { Techniques.carries(it, index, entry.sets.size) }

        /** The next segment, with the load it starts from. */
        fun pendingSegment(technique: TechniqueView, set: SetEntry): SetEntry {
            val previous = parseWeight(set.segments.lastOrNull()?.weight ?: set.weight)
            return SetEntry(weight = Techniques.segmentLoad(technique, previous)?.let(::formatKg).orEmpty())
        }

        fun freshEntry(ex: ExerciseView) = ExerciseEntry(
            ex, List(ex.sets) { SetEntry(weight = proposedLoad(ex)?.let(::formatKg).orEmpty()) }
        )

        /**
         * The load to start with: the engine's target when it has one, else the proposal made from the last time (repeat the
         * heaviest load, or one increment more when every set reached the top of the range), else the last load used.
         */
        fun proposedLoad(ex: ExerciseView): Double? =
            ex.targetLoadKg ?: ex.lastPerformance?.let { last ->
                last.suggestedLoadKg ?: last.sets.mapNotNull { it.weightKg }.maxOrNull()
            }

        /** The sets of a log as the workout screen keeps them while they are edited. */
        fun draftOf(request: LogSessionRequest): List<DraftSet> = request.sets.map {
            DraftSet(
                it.exerciseId, it.setNumber, it.weightKg?.let(::formatKg).orEmpty(), it.repetitions?.toString().orEmpty(),
                it.rir?.let(::formatKg).orEmpty(), it.completed, it.segment ?: 0
            )
        }

        fun buildEntries(session: SessionView, draft: List<DraftSet>): List<ExerciseEntry> = session.exercises.map { ex ->
            val mine = draft.filter { it.exerciseId == ex.exerciseId }
            val saved = mine.filter { it.segment == 0 }.associateBy { it.setNumber }
            val base = freshEntry(ex)
            base.copy(sets = base.sets.mapIndexed { i, set ->
                saved[i + 1]?.let { main ->
                    val segments = mine.filter { it.setNumber == i + 1 && it.segment > 0 }.sortedBy { it.segment }
                    SetEntry(main.weight, main.reps, main.rir, main.done, segments.map { SetEntry(it.weight, it.reps, it.rir, it.done) })
                } ?: set
            })
        }

        /** A weight (or any decimal) as the user's language writes it: "62,5" in Spanish, "62.5" in English. */
        fun formatKg(value: Double): String = com.trazo.android.ui.common.Labels.decimal(value)

        /** Accepts a comma or a dot as decimal separator; returns null for empty or out-of-range input. */
        fun parseWeight(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..1000.0 }
        fun parseReps(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 0..100 }
        fun parseRir(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..10.0 }

        private val skipReasonFor = mapOf(
            "Time" to "time", "Fatigue" to "fatigue", "Equipment" to "equipment",
            "Motivation" to "motivation", "Illness" to "illness", "Other" to "other"
        )

        /**
         * The instant to send when the user says the workout happened on another day: that day at the current time of day, in the
         * device zone. Today (or nothing chosen) sends nothing: the repository stamps the moment the workout ends.
         */
        fun performedAtFor(day: java.time.LocalDate?, now: java.time.ZonedDateTime = java.time.ZonedDateTime.now()): String? {
            if (day == null || day == now.toLocalDate()) return null
            if (day.isAfter(now.toLocalDate())) return null
            return day.atTime(now.toLocalTime()).atZone(now.zone).toInstant().toString()
        }

        fun buildLogRequest(entries: List<ExerciseEntry>, form: FinishForm, now: java.time.ZonedDateTime = java.time.ZonedDateTime.now()): LogSessionRequest {
            val total = entries.sumOf { it.sets.size }
            val done = entries.sumOf { e -> e.sets.count { it.done } }
            val outcome = when {
                done == 0 -> "NotCompleted"
                done == total -> "Completed"
                else -> "Partial"
            }
            val reason = if (outcome == "Completed") "None" else form.reason
            val sets = entries.flatMap { e ->
                e.sets.flatMapIndexed { i, set ->
                    val reps = parseReps(set.reps)
                    val weight = parseWeight(set.weight)
                    val rir = parseRir(set.rir)
                    if (set.done && reps != null) {
                        // Only the segments that were done are sent, numbered without gaps; one not done is not a skipped set.
                        val segments = set.segments.filter { it.done && parseReps(it.reps) != null }.mapIndexed { k, s ->
                            LogSetRequest(e.exercise.exerciseId, i + 1, parseWeight(s.weight), parseReps(s.reps), parseRir(s.rir), true, segment = k + 1)
                        }
                        listOf(LogSetRequest(e.exercise.exerciseId, i + 1, weight, reps, rir, true)) + segments
                    }
                    // A set left undone is reported as skipped, which is what lets the engine see fatigue or lack of time.
                    else listOf(LogSetRequest(e.exercise.exerciseId, i + 1, weight, reps, rir, false, skipReasonFor[reason]))
                }
            }
            return LogSessionRequest(
                outcome = outcome,
                nonCompletionReason = reason,
                recovery = form.recovery,
                sessionRpe = form.sessionRpe,
                painReported = form.pain,
                notes = form.notes.trim().take(1000).ifEmpty { null },
                sets = sets,
                performedAt = performedAtFor(form.performedOn, now),
                // Only exercises with at least one completed set are asked about, and only answered questions are sent.
                exerciseFeedback = form.feedback.filter { (id, answer) ->
                    !answer.isEmpty && entries.any { e -> e.exercise.exerciseId == id && e.sets.any { it.done } }
                }.ifEmpty { null }
            )
        }
    }
}
