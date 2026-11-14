package com.trazo.android.ui.manual

import androidx.lifecycle.ViewModel
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
data class ManualExercise(val id: String, val name: String, val sets: Int = 3, val minReps: Int = 8, val maxReps: Int = 12)

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
    val error: ApiError? = null
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() && days.isNotEmpty() && days.values.all { it.exercises.isNotEmpty() } && !submitting
}

/** Lets the user write their own plan: weekday → routine → exercises with sets and repetitions. Free for every account. */
class ManualPlanViewModel(private val plans: PlanRepository, private val exercises: ExerciseRepository) : ViewModel() {
    private val _state = MutableStateFlow(ManualPlanState())
    val state: StateFlow<ManualPlanState> = _state.asStateFlow()

    fun setName(value: String) = _state.update { it.copy(name = value.take(80)) }
    fun setTotalWeeks(value: Int) = _state.update { it.copy(totalWeeks = value.coerceIn(MIN_WEEKS, MAX_WEEKS)) }

    fun toggleDay(day: String, defaultTitle: String) = _state.update {
        it.copy(days = if (day in it.days) it.days - day else it.days + (day to ManualDay(title = defaultTitle)))
    }

    fun setTitle(day: String, title: String) = updateDay(day) { it.copy(title = title.take(60)) }

    fun removeExercise(day: String, id: String) = updateDay(day) { d -> d.copy(exercises = d.exercises.filterNot { it.id == id }) }

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
        viewModelScope.launch {
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
        viewModelScope.launch {
            when (val result = plans.createManualPlan(buildRequest(s, weekOrder))) {
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
        const val MIN_WEEKS = 1
        const val MAX_WEEKS = 52
        const val MAX_SETS = 10

        /** [weekOrder] is the order the routines are sent in (the week as the user sees it). */
        fun buildRequest(s: ManualPlanState, weekOrder: List<String>) = ManualPlanRequest(
            name = s.name.trim(),
            totalWeeks = s.totalWeeks,
            days = weekOrder.filter { it in s.days }.map { day ->
                val d = s.days.getValue(day)
                ManualDayRequest(day, d.title.trim().ifEmpty { day }, d.exercises.map {
                    ManualExerciseRequest(it.id, it.sets, it.minReps, it.maxReps)
                })
            }
        )
    }
}
