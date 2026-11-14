package com.trazo.android.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.android.data.model.PlanOptions
import com.trazo.android.data.model.PlanProfileRequest
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingState(
    val loadingOptions: Boolean = true,
    val options: PlanOptions? = null,
    val goal: String = "Hypertrophy",
    val experience: String = "Beginner",
    val days: Set<String> = emptySet(),
    val sessionMinutes: Int = 60,
    val totalWeeks: Int = 12,
    val equipment: Set<String> = emptySet(),
    val limitations: Set<String> = emptySet(),
    val submitting: Boolean = false,
    val created: Boolean = false,
    val error: ApiError? = null
) {
    val daysOk: Boolean get() = options?.let { days.size in it.minimumTrainingDays..it.maximumTrainingDays } ?: false
    val canSubmit: Boolean get() = options != null && daysOk && !submitting
}

/** Collects the profile the plans API requires (goal, level, days, session length, equipment, limitations) and creates the plan. */
class OnboardingViewModel(private val plans: PlanRepository) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init { loadOptions() }

    fun loadOptions() {
        _state.update { it.copy(loadingOptions = true, error = null) }
        viewModelScope.launch {
            when (val result = plans.options()) {
                is ApiResult.Success -> _state.update {
                    val o = result.value
                    it.copy(
                        loadingOptions = false, options = o,
                        goal = it.goal.takeIf { g -> g in o.goals } ?: o.goals.firstOrNull() ?: it.goal,
                        experience = it.experience.takeIf { e -> e in o.experience } ?: o.experience.firstOrNull() ?: it.experience,
                        totalWeeks = o.defaultTotalWeeks,
                        sessionMinutes = it.sessionMinutes.coerceIn(o.minimumSessionMinutes, o.maximumSessionMinutes)
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(loadingOptions = false, error = result.error) }
            }
        }
    }

    fun setGoal(value: String) = _state.update { it.copy(goal = value) }
    fun setExperience(value: String) = _state.update { it.copy(experience = value) }
    fun setSessionMinutes(value: Int) = _state.update { it.copy(sessionMinutes = value) }
    fun setTotalWeeks(value: Int) = _state.update { it.copy(totalWeeks = value) }
    fun toggleDay(day: String) = _state.update { it.copy(days = it.days.toggle(day)) }
    fun toggleEquipment(item: String) = _state.update { it.copy(equipment = it.equipment.toggle(item)) }
    fun toggleLimitation(area: String) = _state.update { it.copy(limitations = it.limitations.toggle(area)) }

    fun submit() {
        val s = _state.value
        val options = s.options ?: return
        if (!s.canSubmit) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val request = buildRequest(s, options)
            when (val result = plans.createPlan(request)) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, created = true) }
                is ApiResult.Failure -> _state.update { it.copy(submitting = false, error = result.error) }
            }
        }
    }

    companion object {
        /** "Automatic" only covers 2-4 days, so 5-6 days ask for Push/Pull/Legs; the server may still refine the rest. */
        fun buildRequest(s: OnboardingState, options: PlanOptions) = PlanProfileRequest(
            goal = s.goal,
            experience = s.experience,
            totalWeeks = s.totalWeeks,
            // Keep the order of the week as the server lists it.
            trainingDays = options.days.filter { it in s.days },
            maxSessionMinutes = s.sessionMinutes,
            // Empty means "everything is available" for the server, so an untouched list needs no special case.
            availableEquipment = options.equipment.filter { it in s.equipment },
            limitations = options.limitations.filter { it in s.limitations },
            split = if (s.days.size > 4) "PushPullLegs" else "Automatic"
        )
    }
}

private fun Set<String>.toggle(value: String) = if (value in this) this - value else this + value
