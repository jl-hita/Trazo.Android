package com.trazo.android.ui.manual

import androidx.lifecycle.ViewModel
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.R
import com.trazo.android.diagnostics.launchGuarded
import androidx.lifecycle.viewModelScope
import com.trazo.android.data.model.ExerciseListItem
import com.trazo.android.data.model.ManualDayRequest
import com.trazo.android.data.model.ManualExerciseRequest
import com.trazo.android.data.model.ManualPlanRequest
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.ExerciseFilters
import com.trazo.android.data.repo.ExerciseRepository
import com.trazo.android.data.repo.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** An exercise the user picked for a routine, with the dose they want. */
data class ManualExercise(
    val id: String, val name: String, val sets: Int = 3, val minReps: Int = 8, val maxReps: Int = 12,
    /** Done as a superset with the exercise that follows it. */
    val linkedToNext: Boolean = false,
    /** Kept from the stored plan when editing, so saving does not reset them. */
    val targetRir: Double? = null, val restSeconds: Int? = null
)

data class ManualDay(val title: String = "", val exercises: List<ManualExercise> = emptyList())

data class ManualPlanState(
    val name: String = "",
    val totalWeeks: Int = 8,
    /** Selected weekdays (English ids, as the API names them) and their routine. */
    val days: Map<String, ManualDay> = emptyMap(),
    /** The weekday whose exercise picker is open, if any. */
    val picking: String? = null,
    val query: String = "",
    val results: List<ExerciseListItem> = emptyList(),
    val searching: Boolean = false,
    val submitting: Boolean = false,
    val created: Boolean = false,
    val error: ApiError? = null,
    /** Editing the active plan instead of creating one. */
    val editing: Boolean = false,
    val loadingExisting: Boolean = false,
    val notes: String? = null,
    /** "Calendar" (routines on weekdays, repeated every week) or "Sequence" (routines that rotate in order, no weekdays). */
    val schedulingMode: String = "Calendar",
    /** The server announces the sequence mode: only then it is offered when creating. */
    val sequenceAvailable: Boolean = false,
    /** Counter for the keys of the routines of a sequence plan, so a key is never reused after removing one. */
    val nextRoutine: Int = 1,
    /**
     * The order of the routines of a sequence plan. It is a list on purpose: [days] is a map, and maps with the same entries are
     * equal whatever their order, so a reordering kept only there would never reach the state flow.
     */
    val order: List<String> = emptyList()
) {
    val isSequence: Boolean get() = schedulingMode == "Sequence"
    val canSubmit: Boolean
        get() = name.isNotBlank() && days.isNotEmpty() && days.values.all { it.exercises.isNotEmpty() } && !submitting
}

/** Lets the user write their own plan: weekday → routine → exercises with sets and repetitions. Free for every account. */
class ManualPlanViewModel(
    private val plans: PlanRepository, private val exercises: ExerciseRepository, editing: Boolean = false
) : ViewModel() {
    private val _state = MutableStateFlow(ManualPlanState(editing = editing, loadingExisting = editing))
    val state: StateFlow<ManualPlanState> = _state.asStateFlow()

    init { if (editing) loadExisting() else loadOptions() }

    private fun loadOptions() {
        viewModelScope.launchGuarded(R.string.feature_manual_plan, onFailure = { }) {
            val options = (plans.options() as? ApiResult.Success)?.value
            _state.update { it.copy(sequenceAvailable = options != null && "Sequence" in options.schedulingModes) }
        }
    }

    /** Fills the editor with the routines of the active plan: the days and exercises of the current week. */
    private fun loadExisting() {
        viewModelScope.launchGuarded(R.string.feature_manual_plan, onFailure = { _state.update { it.copy(loadingExisting = false, error = ApiError(ErrorKind.Unknown)) } }) {
            val plan = (plans.currentPlan() as? ApiResult.Success)?.value?.value
            when (val week = plans.currentWeek()) {
                is ApiResult.Failure -> _state.update { it.copy(loadingExisting = false, error = week.error) }
                is ApiResult.Success -> _state.update {
                    val sequence = plan?.schedulingMode == "Sequence"
                    val routines = routinesOf(week.value.value.sessions, sequence)
                    it.copy(
                        loadingExisting = false, name = plan?.macrocycle?.name.orEmpty(), notes = plan?.macrocycle?.notes,
                        totalWeeks = plan?.totalWeeks ?: it.totalWeeks, days = routines,
                        schedulingMode = if (sequence) "Sequence" else "Calendar", nextRoutine = routines.size + 1,
                        order = if (sequence) routines.keys.toList() else emptyList()
                    )
                }
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value.take(80)) }
    fun setTotalWeeks(value: Int) = _state.update { it.copy(totalWeeks = value.coerceIn(MIN_WEEKS, MAX_WEEKS)) }

    /** Only when creating: an existing plan keeps its way of scheduling. Changing it starts the routines again. */
    fun setSchedulingMode(mode: String) = _state.update {
        if (it.editing || it.schedulingMode == mode || (mode == "Sequence" && !it.sequenceAvailable)) it
        else it.copy(schedulingMode = mode, days = emptyMap(), nextRoutine = 1, order = emptyList())
    }

    /** Adds a routine at the end of the rotation of a sequence plan. */
    fun addRoutine(defaultTitle: String) = _state.update {
        if (!it.isSequence || it.days.size >= MAX_ROUTINES) it
        else it.copy(days = it.days + ("r${it.nextRoutine}" to ManualDay(title = defaultTitle)), order = it.order + "r${it.nextRoutine}", nextRoutine = it.nextRoutine + 1)
    }

    fun removeRoutine(key: String) = _state.update { it.copy(days = it.days - key, order = it.order - key) }

    /** Moves a routine earlier (-1) or later (+1) in the rotation. */
    fun moveRoutine(key: String, delta: Int) = _state.update {
        val keys = it.order.toMutableList()
        val from = keys.indexOf(key)
        val to = from + delta
        if (from < 0 || to !in keys.indices) it
        else {
            keys.add(to, keys.removeAt(from))
            it.copy(order = keys)
        }
    }

    fun toggleDay(day: String, defaultTitle: String) = _state.update {
        it.copy(days = if (day in it.days) it.days - day else it.days + (day to ManualDay(title = defaultTitle)))
    }

    fun setTitle(day: String, title: String) = updateDay(day) { it.copy(title = title.take(60)) }

    fun removeExercise(day: String, id: String) = updateDay(day) { d -> d.copy(exercises = normalizeLinks(d.exercises.filterNot { it.id == id })) }

    /** Joins an exercise with the next one in a superset (or splits them). A superset has two or three exercises. */
    fun toggleLink(day: String, id: String) = updateDay(day) { d ->
        val index = d.exercises.indexOfFirst { it.id == id }
        if (index < 0 || index == d.exercises.lastIndex) return@updateDay d
        val linked = !d.exercises[index].linkedToNext
        val next = d.exercises.mapIndexed { i, e -> if (i == index) e.copy(linkedToNext = linked) else e }
        if (linked && runLength(next, index) > MAX_SUPERSET) d else d.copy(exercises = next)
    }

    fun changeSets(day: String, id: String, delta: Int) = updateExercise(day, id) { it.copy(sets = (it.sets + delta).coerceIn(1, MAX_SETS)) }

    fun changeReps(day: String, id: String, delta: Int) = updateExercise(day, id) {
        val min = (it.minReps + delta).coerceIn(1, 50)
        it.copy(minReps = min, maxReps = maxOf(min, (it.maxReps + delta).coerceIn(1, 100)))
    }

    fun openPicker(day: String) {
        _state.update { it.copy(picking = day, query = "", results = emptyList()) }
        search("")
    }

    fun closePicker() = _state.update { it.copy(picking = null, query = "", results = emptyList(), searching = false) }

    fun search(query: String) {
        _state.update { it.copy(query = query, searching = true) }
        viewModelScope.launchGuarded(R.string.feature_manual_plan, onFailure = { _state.update { it.copy(results = emptyList(), searching = false) } }) {
            val result = exercises.search(ExerciseFilters(query = query), page = 1, pageSize = 20)
            _state.update {
                // A newer query may have been typed meanwhile: ignore the stale answer.
                if (it.query != query) it
                else when (result) {
                    is ApiResult.Success -> it.copy(results = result.value.items, searching = false)
                    is ApiResult.Failure -> it.copy(results = emptyList(), searching = false)
                }
            }
        }
    }

    /** Adds the exercise to the open routine; an exercise already in it is not added twice. */
    fun pick(item: ExerciseListItem, displayName: String) {
        val day = _state.value.picking ?: return
        updateDay(day) { d ->
            if (d.exercises.any { it.id == item.id }) d else d.copy(exercises = d.exercises + ManualExercise(item.id, displayName))
        }
        closePicker()
    }

    fun submit(weekOrder: List<String>) {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launchGuarded(R.string.feature_manual_plan, onFailure = { _state.update { it.copy(submitting = false, error = ApiError(ErrorKind.Unknown)) } }) {
            val request = buildRequest(s, weekOrder)
            when (val result = if (s.editing) plans.updateManualPlan(request) else plans.createManualPlan(request)) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, created = true) }
                is ApiResult.Failure -> _state.update { it.copy(submitting = false, error = result.error) }
            }
        }
    }

    private fun updateDay(day: String, transform: (ManualDay) -> ManualDay) =
        _state.update { s -> s.days[day]?.let { s.copy(days = s.days + (day to transform(it))) } ?: s }

    private fun updateExercise(day: String, id: String, transform: (ManualExercise) -> ManualExercise) =
        updateDay(day) { d -> d.copy(exercises = d.exercises.map { if (it.id == id) transform(it) else it }) }

    companion object {
        const val MAX_SUPERSET = 3

        /** The routines of the sessions of a week, one per weekday, with the supersets as links between consecutive exercises. */
        fun routinesOf(sessions: List<com.trazo.android.data.model.SessionView>, sequence: Boolean = false): Map<String, ManualDay> =
            // A sequence plan has no weekdays: the routines are the sessions of one period, in program order, under their own keys.
            (if (sequence) sessions.sortedBy { it.sequenceIndex }.mapIndexed { i, s -> "r${i + 1}" to s }
            else sessions.filter { it.day != null }.map { it.day!! to it }).associate { (key, session) ->
                val list = session.exercises.sortedBy { it.order }
                key to ManualDay(
                    session.title,
                    list.mapIndexed { i, e ->
                        ManualExercise(
                            e.exerciseId, e.name, e.sets, e.minRepetitions, e.maxRepetitions,
                            linkedToNext = e.supersetGroup != null && list.getOrNull(i + 1)?.supersetGroup == e.supersetGroup,
                            targetRir = e.targetRir, restSeconds = e.restSeconds
                        )
                    }
                )
            }
        const val MIN_WEEKS = 1
        const val MAX_WEEKS = 52
        const val MAX_SETS = 10
        const val MAX_ROUTINES = 7

        /** Length of the superset run that contains [index]. */
        fun runLength(list: List<ManualExercise>, index: Int): Int {
            var start = index
            while (start > 0 && list[start - 1].linkedToNext) start--
            var end = index
            while (end < list.lastIndex && list[end].linkedToNext) end++
            return end - start + 1
        }

        /** The last exercise cannot link to a next one. */
        fun normalizeLinks(list: List<ManualExercise>): List<ManualExercise> =
            list.mapIndexed { i, e -> if (i == list.lastIndex && e.linkedToNext) e.copy(linkedToNext = false) else e }

        /** Letters A, B, C... for each run of linked exercises; exercises outside a run get none. */
        fun supersetGroups(list: List<ManualExercise>): List<String?> {
            val groups = arrayOfNulls<String>(list.size)
            var letter = 'A'
            var i = 0
            while (i < list.size) {
                var j = i
                while (j < list.lastIndex && list[j].linkedToNext) j++
                if (j > i) { for (k in i..j) groups[k] = letter.toString(); letter++ }
                i = j + 1
            }
            return groups.toList()
        }

        /** [weekOrder] is the order the routines are sent in (the week as the user sees it). */
        fun buildRequest(s: ManualPlanState, weekOrder: List<String>) = ManualPlanRequest(
            name = s.name.trim(),
            totalWeeks = s.totalWeeks,
            notes = s.notes,
            schedulingMode = if (s.isSequence) "Sequence" else null,
            days = (if (s.isSequence) s.order.filter { it in s.days } else weekOrder.filter { it in s.days }).mapIndexed { index, day ->
                val d = s.days.getValue(day)
                val groups = supersetGroups(d.exercises)
                // A sequence routine has no weekday to send, nor to name it by: it falls back to its place in the rotation.
                ManualDayRequest(if (s.isSequence) null else day, d.title.trim().ifEmpty { if (s.isSequence) "R${index + 1}" else day }, d.exercises.mapIndexed { i, it ->
                    ManualExerciseRequest(it.id, it.sets, it.minReps, it.maxReps, it.targetRir, it.restSeconds, groups[i])
                })
            }
        )
    }
}
