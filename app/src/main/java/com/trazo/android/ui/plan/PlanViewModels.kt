package com.trazo.android.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.android.data.model.*
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.PlanRepository
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
    val fromCache: Boolean = false,
    val error: ApiError? = null
)

/** The "Plan" tab: current week with its sessions and the macrocycle summary. */
class PlanViewModel(private val plans: PlanRepository) : ViewModel() {
    private val _state = MutableStateFlow(PlanUiState())
    val state: StateFlow<PlanUiState> = _state.asStateFlow()
    val pendingLogs: StateFlow<Int> = plans.pendingCount

    init { load() }

    /** The plan is over: closes it (history is kept) so the onboarding can create the next one. */
    fun startNextPlan() {
        viewModelScope.launch {
            plans.archive()
            load()
        }
    }

    fun sendPending() { viewModelScope.launch { if (plans.flushPending() > 0) load() } }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val week = plans.currentWeek()) {
                is ApiResult.Failure -> {
                    val noPlan = week.error.code == PlanRepository.NO_ACTIVE_PLAN
                    _state.value = PlanUiState(loading = false, needsOnboarding = noPlan, error = week.error.takeUnless { noPlan })
                }
                is ApiResult.Success -> {
                    val plan = (plans.currentPlan() as? ApiResult.Success)?.value
                    _state.value = PlanUiState(
                        loading = false, plan = plan?.value, week = week.value.value,
                        fromCache = week.value.fromCache || plan?.fromCache == true
                    )
                }
            }
        }
    }
}

data class HistoryUiState(val loading: Boolean = true, val items: List<HistoryItem> = emptyList(), val error: ApiError? = null)

class HistoryViewModel(private val plans: PlanRepository) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = HistoryUiState()
        viewModelScope.launch {
            _state.value = when (val result = plans.history()) {
                is ApiResult.Success -> HistoryUiState(false, result.value)
                is ApiResult.Failure -> HistoryUiState(false, error = result.error)
            }
        }
    }
}
