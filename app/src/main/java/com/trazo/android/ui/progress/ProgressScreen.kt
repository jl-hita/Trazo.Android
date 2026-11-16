package com.trazo.android.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trazo.android.R
import com.trazo.android.data.model.ExercisePoint
import com.trazo.android.data.model.ExerciseProgress
import com.trazo.android.data.model.MuscleVolume
import com.trazo.android.data.model.ProgressView
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.data.repo.PlanRepository
import com.trazo.android.diagnostics.launchGuarded
import com.trazo.android.ui.common.*
import com.trazo.android.ui.plan.formatDate
import com.trazo.android.ui.theme.TrazoPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class ProgressUiState(val loading: Boolean = true, val progress: ProgressView? = null, val error: ApiError? = null)

/** What the user has achieved over time, across every plan: records, exercise history, weekly sets per muscle and the streak. */
class ProgressViewModel(private val plans: PlanRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProgressUiState())
    val state: StateFlow<ProgressUiState> = _state.asStateFlow()

    fun load() {
        // Keep what is on screen while it refreshes; only the first load shows the spinner.
        _state.value = _state.value.copy(loading = _state.value.progress == null, error = null)
        viewModelScope.launchGuarded(R.string.feature_progress, onFailure = { _state.value = ProgressUiState(false, error = ApiError(ErrorKind.Unknown)) }) {
            _state.value = when (val r = plans.progress()) {
                is ApiResult.Success -> ProgressUiState(false, r.value)
                is ApiResult.Failure -> ProgressUiState(false, _state.value.progress, r.error)
            }
        }
    }
}

internal fun formatKgText(value: Double?): String =
    if (value == null) "–" else if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.getDefault(), "%.1f", value)

@Composable
fun ProgressScreen(viewModel: ProgressViewModel) {
    LaunchedEffect(Unit) { viewModel.load() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(R.string.progress_title))
        val progress = state.progress
        when {
            state.loading && progress == null -> LoadingBox()
            progress == null -> ErrorBox(state.error ?: ApiError(ErrorKind.Unknown), viewModel::load)
            progress.summary.totalSessions == 0 -> EmptyBox(stringResource(R.string.progress_empty))
            else -> ProgressContent(progress)
        }
    }
}

@Composable
private fun ProgressContent(progress: ProgressView) {
    val s = progress.summary
    val records = progress.exercises.filter { it.newRecord }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), stringResource(R.string.progress_streak), stringResource(R.string.progress_weeks_value, s.streakWeeks))
                StatCard(
                    Modifier.weight(1f), stringResource(R.string.progress_this_week),
                    if (s.weeklyGoal > 0) stringResource(R.string.progress_of_goal, s.sessionsThisWeek, s.weeklyGoal) else s.sessionsThisWeek.toString()
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), stringResource(R.string.progress_last_30), s.sessionsLast30Days.toString())
                StatCard(Modifier.weight(1f), stringResource(R.string.progress_volume), stringResource(R.string.progress_tons, s.totalVolumeKg / 1000.0))
            }
        }
        if (records.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.progress_records)) }
            items(records, key = { "record-" + it.exerciseId }) { e ->
                TrazoCard {
                    Text(stringResource(R.string.progress_new_record), color = TrazoPalette.Ok, style = MaterialTheme.typography.labelLarge)
                    Text(e.name, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.progress_best_1rm, formatKgText(e.bestEstimatedOneRepMaxKg)), color = TrazoPalette.TextSecondary)
                }
            }
        }
        if (progress.muscles.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.progress_muscles)) }
            item { Text(stringResource(R.string.progress_muscles_note), color = TrazoPalette.Muted, style = MaterialTheme.typography.bodySmall) }
            item {
                TrazoCard { progress.muscles.take(12).forEach { MuscleRow(it) } }
            }
        }
        if (progress.exercises.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.progress_exercises)) }
            items(progress.exercises, key = { it.exerciseId }) { e ->
                ExerciseCard(e, expanded = open == e.exerciseId, onToggle = { open = if (open == e.exerciseId) null else e.exerciseId })
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String) {
    TrazoCard(modifier) {
        Text(label, color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.headlineSmall)
    }
}

/** This week's effective sets, with the last eight weeks as small bars so the trend is visible at a glance. */
@Composable
private fun MuscleRow(m: MuscleVolume) {
    val color = TrazoPalette.PrimaryButton
    val current = TrazoPalette.Ok
    val top = (m.setsByWeek.maxOrNull() ?: 0.0).coerceAtLeast(1.0).toFloat()
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(Labels.muscle(m.muscle), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.progress_sets_this_week, formatKgText(m.setsByWeek.lastOrNull())),
                color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall
            )
        }
        Canvas(Modifier.width(120.dp).height(32.dp)) {
            val n = m.setsByWeek.size
            val slot = size.width / n
            m.setsByWeek.forEachIndexed { i, v ->
                val h = (v.toFloat() / top) * size.height
                drawRect(if (i == n - 1) current else color.copy(alpha = 0.6f), Offset(slot * i + slot * 0.15f, size.height - h), Size(slot * 0.7f, h.coerceAtLeast(if (v > 0) 2f else 0f)))
            }
        }
    }
}

@Composable
private fun ExerciseCard(e: ExerciseProgress, expanded: Boolean, onToggle: () -> Unit) {
    TrazoCard(onClick = onToggle) {
        Text(e.name, style = MaterialTheme.typography.titleMedium)
        val best = if (e.bestEstimatedOneRepMaxKg != null) stringResource(R.string.progress_best_1rm, formatKgText(e.bestEstimatedOneRepMaxKg))
        else e.points.maxOfOrNull { it.repsAtTop }?.let { stringResource(R.string.progress_best_reps, it) } ?: ""
        Text(
            listOf(best, stringResource(R.string.progress_sessions, e.sessions), formatDate(e.lastDate)).filter { it.isNotEmpty() }.joinToString(" · "),
            color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall
        )
        if (expanded) {
            if (e.points.size > 1) ExerciseChart(e.points, useEstimate = e.bestEstimatedOneRepMaxKg != null)
            e.points.takeLast(5).reversed().forEach { p ->
                Text(
                    formatDate(p.date) + ": " + (if (p.topWeightKg > 0) formatKgText(p.topWeightKg) + " kg × " else "") + p.repsAtTop +
                        " · " + stringResource(R.string.progress_sets, p.sets),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** One line over the workouts of an exercise: estimated 1RM (or repetitions for bodyweight work). */
@Composable
private fun ExerciseChart(points: List<ExercisePoint>, useEstimate: Boolean) {
    val values = points.map { (if (useEstimate) it.estimatedOneRepMaxKg ?: it.topWeightKg else it.repsAtTop.toDouble()).toFloat() }
    val lo = values.min()
    val hi = values.max()
    val line = TrazoPalette.Ok
    val grid = TrazoPalette.Muted.copy(alpha = 0.35f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(if (useEstimate) R.string.progress_chart_1rm else R.string.progress_chart_reps),
            color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall
        )
        Row(Modifier.fillMaxWidth().height(110.dp)) {
            Column(Modifier.fillMaxHeight().padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                Text(formatKgText(hi.toDouble()), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
                Text(formatKgText(lo.toDouble()), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Canvas(Modifier.weight(1f).fillMaxHeight()) {
                val span = (hi - lo).takeIf { it > 0f } ?: 1f
                fun x(i: Int) = if (values.size == 1) size.width / 2 else size.width * i / (values.size - 1)
                fun y(v: Float) = size.height - 8f - (v - lo) / span * (size.height - 16f)
                drawLine(grid, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
                drawLine(grid, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 2f)
                for (i in 0 until values.size - 1) drawLine(line, Offset(x(i), y(values[i])), Offset(x(i + 1), y(values[i + 1])), strokeWidth = 5f, cap = StrokeCap.Round)
                values.forEachIndexed { i, v -> drawCircle(line, radius = 7f, center = Offset(x(i), y(v))) }
            }
        }
    }
}
