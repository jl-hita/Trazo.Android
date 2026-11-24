package com.trazo.android.ui.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trazo.android.R
import com.trazo.android.data.model.BlockView
import com.trazo.android.data.model.BlockWeekView
import com.trazo.android.data.model.ExerciseView
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.data.repo.PlanRepository
import com.trazo.android.diagnostics.launchGuarded
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoPalette
import com.trazo.android.ui.workout.WorkoutViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BlockUiState(val loading: Boolean = true, val block: BlockView? = null, val error: ApiError? = null)

/** The whole current block (about a month), so the user sees where the training is going. */
class BlockViewModel(private val plans: PlanRepository) : ViewModel() {
    private val _state = MutableStateFlow(BlockUiState())
    val state: StateFlow<BlockUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = BlockUiState()
        viewModelScope.launchGuarded(R.string.feature_overview, onFailure = { _state.value = BlockUiState(false, error = ApiError(ErrorKind.Unknown)) }) {
            _state.value = when (val r = plans.block()) {
                is ApiResult.Success -> BlockUiState(false, r.value)
                is ApiResult.Failure -> BlockUiState(false, error = r.error)
            }
        }
    }
}

@Composable
fun BlockScreen(viewModel: BlockViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(R.string.block_title), onBack)
        val block = state.block
        when {
            state.loading -> LoadingBox()
            block == null -> ErrorBox(state.error ?: ApiError(ErrorKind.Unknown), viewModel::load)
            block.weeks.isEmpty() -> EmptyBox(stringResource(R.string.block_empty))
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                block.mesocycle?.let { m ->
                    item {
                        val phase = Labels.phase(m.phaseType) + if (m.isDeload) " · ${stringResource(R.string.plan_deload)}" else ""
                        val sequence = block.weeks.firstOrNull()?.weekStart == null
                        Text(stringResource(if (sequence) R.string.plan_block_cycle else R.string.plan_block, m.sequence, phase, m.weekInBlock, m.weeks), color = TrazoPalette.TextSecondary)
                        Text(stringResource(R.string.block_note), color = TrazoPalette.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                items(block.weeks, key = { it.weekNumber }) { WeekSection(it) }
            }
        }
    }
}

@Composable
private fun WeekSection(week: BlockWeekView) {
    val sets = week.sessions.sumOf { s -> s.exercises.sumOf { it.sets } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(
            // No dates means a sequence plan, whose periods are cycles.
            stringResource(if (week.weekStart == null) R.string.block_cycle else R.string.block_week, week.weekNumber) + if (week.isCurrent) " · " + stringResource(R.string.block_current) else ""
        )
        val range = if (week.weekStart != null && week.weekEnd != null) "${formatDate(week.weekStart)} – ${formatDate(week.weekEnd)} · " else ""
        Text(range + stringResource(R.string.block_week_sets, sets), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
        week.sessions.forEach { session ->
            TrazoCard {
                Text(session.title, style = MaterialTheme.typography.titleMedium)
                Text(sessionWhen(session), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                session.exercises.forEach { Text("• " + exerciseLine(it), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun exerciseLine(e: ExerciseView): String =
    e.name + (e.supersetGroup?.let { " [$it]" } ?: "") + " — " + stringResource(R.string.session_sets_reps, e.sets, e.minRepetitions, e.maxRepetitions) +
        (e.targetRir?.let { " · " + stringResource(R.string.session_rir, WorkoutViewModel.formatKg(it)) } ?: "")
