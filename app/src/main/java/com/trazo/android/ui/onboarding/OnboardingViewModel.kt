package com.trazo.android.ui.onboarding

import androidx.lifecycle.ViewModel
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.R
import com.trazo.android.diagnostics.launchGuarded
import androidx.lifecycle.viewModelScope
import com.trazo.android.data.model.PlanOptions
import com.trazo.android.data.model.PlanProfileRequest
import com.trazo.android.data.model.ProgramSheetView
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.PlanRepository
import com.trazo.android.ui.plan.MusclePriorities
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
    /** "Calendar" (fixed days of the week) or "Sequence" (no fixed days: the program follows the order of its sessions). */
    val schedulingMode: String = "Calendar",
    /** Sessions of one full pass through the programme in a sequence plan. */
    val sessionsPerPeriod: Int = 3,
    val sessionMinutes: Int = 60,
    val totalWeeks: Int = 12,
    /** First day of the plan; null means today (the server's default). */
    val startDate: java.time.LocalDate? = null,
    val equipment: Set<String> = emptySet(),
    val limitations: Set<String> = emptySet(),
    val split: String = "Automatic",
    /** Muscle to "Emphasize" / "Maintain"; a muscle that is absent is "Grow". Only sent for a hypertrophy plan. */
    val musclePriorities: Map<String, String> = emptyMap(),
    /** "Regular", "New" or "Returning": the last two start the plan with a few weeks of preparation. */
    val readiness: String = PreparationChoice.REGULAR,
    /** Only for "Returning": the break lasted the months the server marks as long, which suggests a longer preparation. */
    val longBreak: Boolean = false,
    /** Weeks of preparation, inside [totalWeeks]; zero for a regular plan. */
    val preparationWeeks: Int = 0,
    val includeDeload: Boolean = true,
    /** Training weeks between deloads, or null to let the server place them. */
    val deloadEvery: Int? = null,
    val submitting: Boolean = false,
    val created: Boolean = false,
    val error: ApiError? = null,
    /** Starting a ready-made program: what it fixes is not asked, and its sheet is shown. */
    val program: ProgramSheetView? = null
) {
    val isSequence: Boolean get() = schedulingMode == "Sequence"
    val daysOk: Boolean get() = options?.let {
        if (isSequence) sessionsPerPeriod in it.minimumSessionsPerPeriod..it.maximumSessionsPerPeriod
        else if (program != null) days.size == program.program.sessionsPerWeek
        else days.size in it.minimumTrainingDays..it.maximumTrainingDays
    } ?: false
    val canSubmit: Boolean get() = options != null && daysOk && !submitting
}

/** Collects the profile the plans API requires (goal, level, days, session length, equipment, limitations) and creates the plan. */
class OnboardingViewModel(private val plans: PlanRepository, private val programId: String? = null) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init { loadOptions() }

    fun loadOptions() {
        _state.update { it.copy(loadingOptions = true, error = null) }
        viewModelScope.launchGuarded(R.string.feature_onboarding, onFailure = { _state.update { it.copy(loadingOptions = false, error = ApiError(ErrorKind.Unknown)) } }) {
            when (val result = plans.options()) {
                is ApiResult.Success -> {
                _state.update {
                    val o = result.value
                    it.copy(
                        loadingOptions = false, options = o,
                        goal = it.goal.takeIf { g -> g in o.goals } ?: o.goals.firstOrNull() ?: it.goal,
                        experience = it.experience.takeIf { e -> e in o.experience } ?: o.experience.firstOrNull() ?: it.experience,
                        totalWeeks = o.defaultTotalWeeks,
                        sessionMinutes = it.sessionMinutes.coerceIn(o.minimumSessionMinutes, o.maximumSessionMinutes)
                    )
                }
                if (programId != null) loadSheet(programId, first = true)
                }
                is ApiResult.Failure -> _state.update { it.copy(loadingOptions = false, error = result.error) }
            }
        }
    }

    /** The sheet for the equipment chosen. The first time, what the program fixes becomes the form's starting values. */
    private suspend fun loadSheet(id: String, first: Boolean) {
        val s = _state.value
        val options = s.options ?: return
        val request = PlanProfileRequest(
            goal = "Hypertrophy", experience = s.experience, availableEquipment = options.equipment.filter { it in s.equipment },
            limitations = options.limitations.filter { it in s.limitations }
        )
        when (val result = plans.programSheet(id, request)) {
            is ApiResult.Success -> _state.update {
                val p = result.value.program
                if (!first) it.copy(program = result.value, error = null)
                else it.copy(
                    program = result.value, goal = "Hypertrophy", experience = p.minimumExperience, schedulingMode = p.schedulingMode,
                    sessionsPerPeriod = p.sessionsPerWeek, sessionMinutes = p.sessionMinutes, totalWeeks = p.totalWeeks,
                    days = defaultDays(options.days, p.sessionsPerWeek)
                )
            }
            is ApiResult.Failure -> _state.update { if (first) it.copy(options = null, error = result.error) else it.copy(error = result.error) }
        }
    }

    /** After the equipment or the injured areas change. */
    fun refreshProgram() {
        val id = programId ?: return
        viewModelScope.launchGuarded(R.string.feature_onboarding, onFailure = { }) { loadSheet(id, first = false) }
    }

    fun setGoal(value: String) = _state.update { it.copy(goal = value) }
    fun setExperience(value: String) = _state.update { it.copy(experience = value) }
    fun setSplit(value: String) = _state.update { it.copy(split = value) }
    fun setIncludeDeload(value: Boolean) = _state.update { it.copy(includeDeload = value) }
    fun setDeloadEvery(value: Int?) = _state.update { it.copy(deloadEvery = value) }
    fun setStartDate(value: java.time.LocalDate?) = _state.update { it.copy(startDate = value) }
    fun setSessionMinutes(value: Int) = _state.update { it.copy(sessionMinutes = value) }
    fun setTotalWeeks(value: Int) = _state.update { s ->
        val o = s.options
        val prep = o?.preparation
        val min = if (o != null && prep != null) PreparationChoice.minimumTotalWeeks(o.minimumTotalWeeks, s.preparationWeeks, prep) else 0
        s.copy(totalWeeks = maxOf(value, min))
    }
    fun setReadiness(value: String) = _state.update { applyPreparation(it, value, it.longBreak, null) }
    fun setLongBreak(value: Boolean) = _state.update { applyPreparation(it, it.readiness, value, null) }
    fun setPreparationWeeks(value: Int) = _state.update { applyPreparation(it, it.readiness, it.longBreak, value) }
    fun setSchedulingMode(value: String) = _state.update { it.copy(schedulingMode = value) }
    fun setSessionsPerPeriod(value: Int) = _state.update { it.copy(sessionsPerPeriod = value) }
    fun setMusclePriority(muscle: String, priority: String) =
        _state.update { it.copy(musclePriorities = MusclePriorities.with(it.musclePriorities, muscle, priority)) }
    fun toggleDay(day: String) = _state.update { it.copy(days = it.days.toggle(day)) }
    fun toggleEquipment(item: String) = _state.update { it.copy(equipment = it.equipment.toggle(item)) }
    fun toggleLimitation(area: String) = _state.update { it.copy(limitations = it.limitations.toggle(area)) }

    fun submit() {
        val s = _state.value
        val options = s.options ?: return
        if (!s.canSubmit) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launchGuarded(R.string.feature_onboarding, onFailure = { _state.update { it.copy(submitting = false, error = ApiError(ErrorKind.Unknown)) } }) {
            val request = buildRequest(s, options)
            when (val result = if (programId != null) plans.createProgramPlan(programId, request) else plans.createPlan(request)) {
                is ApiResult.Success -> _state.update { it.copy(submitting = false, created = true) }
                is ApiResult.Failure -> _state.update { it.copy(submitting = false, error = result.error) }
            }
        }
    }

    companion object {
        /** Evenly spread days of the week for a program of [count] sessions. */
        fun defaultDays(week: List<String>, count: Int): Set<String> {
            val places = when (count) { 2 -> listOf(0, 3); 3 -> listOf(0, 2, 4); 4 -> listOf(0, 1, 3, 4); 5 -> listOf(0, 1, 3, 4, 5); else -> (0 until count).toList() }
            return places.mapNotNull { week.getOrNull(it) }.toSet()
        }

        /** Sets the starting point and its weeks (the suggestion unless [weeks] is given) and lengthens the plan when it would be too short. */
        fun applyPreparation(s: OnboardingState, readiness: String, longBreak: Boolean, weeks: Int?): OnboardingState {
            val prep = s.options?.preparation ?: return s
            val chosen = if (readiness == PreparationChoice.REGULAR) 0
            else (weeks ?: PreparationChoice.suggestedWeeks(readiness, longBreak, prep)).coerceIn(prep.minimumWeeks, prep.maximumWeeks)
            val min = PreparationChoice.minimumTotalWeeks(s.options.minimumTotalWeeks, chosen, prep)
            return s.copy(readiness = readiness, longBreak = longBreak && readiness == PreparationChoice.RETURNING, preparationWeeks = chosen,
                totalWeeks = maxOf(s.totalWeeks, min))
        }

        /** An explicit split is sent as chosen. "Automatic" only covers 2-4 days, so 5-6 days ask for Push/Pull/Legs; the server may still refine the rest. */
        fun buildRequest(s: OnboardingState, options: PlanOptions) = PlanProfileRequest(
            goal = s.goal,
            experience = s.experience,
            totalWeeks = s.totalWeeks,
            startDate = s.startDate?.toString(),
            // Keep the order of the week as the server lists it. A sequence plan has no days: it sends the sessions of one pass instead.
            trainingDays = if (s.isSequence) null else options.days.filter { it in s.days },
            schedulingMode = if (s.isSequence) "Sequence" else null,
            sessionsPerPeriod = if (s.isSequence) s.sessionsPerPeriod else null,
            maxSessionMinutes = s.sessionMinutes,
            // Empty means "everything is available" for the server, so an untouched list needs no special case.
            availableEquipment = options.equipment.filter { it in s.equipment },
            limitations = options.limitations.filter { it in s.limitations },
            includeDeloadPhases = s.includeDeload,
            // Null lets the server spread the deloads so the training blocks stay full.
            deloadEveryTrainingWeeks = s.deloadEvery.takeIf { s.includeDeload },
            musclePriorities = MusclePriorities.forRequest(s.goal, s.musclePriorities),
            // Only a server that offers the preparation understands it; a regular plan sends nothing.
            readiness = s.readiness.takeIf { options.preparation != null && s.preparationWeeks > 0 },
            preparationWeeks = s.preparationWeeks.takeIf { options.preparation != null && it > 0 },
            split = if (s.split == "Automatic" && (if (s.isSequence) s.sessionsPerPeriod else s.days.size) > 4) "PushPullLegs" else s.split
        )
    }
}

private fun Set<String>.toggle(value: String) = if (value in this) this - value else this + value
