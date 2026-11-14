package com.trazo.android.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.android.R
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoField
import com.trazo.android.ui.theme.TrazoLink
import com.trazo.android.ui.theme.TrazoPalette

@Composable
fun WorkoutScreen(viewModel: WorkoutViewModel, onExit: () -> Unit, onExercise: (String) -> Unit) {
    SecureScreen()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmLeave by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }
    val done = state.finish as? FinishState.Done

    BackHandler(enabled = done == null) { confirmLeave = true }

    if (done != null) { FinishedScreen(done, onExit); return }

    Column(Modifier.fillMaxSize().imePadding()) {
        TrazoTopBar(state.session?.title ?: stringResource(R.string.workout_title), onBack = { confirmLeave = true })
        if (state.session == null) { EmptyBox(stringResource(R.string.err_not_found)); return@Column }
        Text(
            stringResource(R.string.workout_progress, state.doneSets, state.allSets),
            color = TrazoPalette.TextSecondary, modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.entries, key = { it.exercise.exerciseId }) { entry ->
                TrazoCard {
                    Text(entry.exercise.name, style = MaterialTheme.typography.titleMedium)
                    val ex = entry.exercise
                    Text(
                        stringResource(R.string.session_sets_reps, ex.sets, ex.minRepetitions, ex.maxRepetitions) +
                            (ex.targetRir?.let { " · " + stringResource(R.string.session_rir, WorkoutViewModel.formatKg(it)) } ?: "") +
                            (ex.targetLoadKg?.let { " · " + stringResource(R.string.session_target_load, WorkoutViewModel.formatKg(it)) } ?: ""),
                        color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall
                    )
                    entry.sets.forEachIndexed { index, set ->
                        SetRow(
                            index + 1, set,
                            onChange = { t -> viewModel.updateSet(ex.exerciseId, index, t) },
                            onToggle = { viewModel.toggleDone(ex.exerciseId, index) }
                        )
                    }
                    Row {
                        TrazoLink(onClick = { viewModel.showAlternatives(ex.exerciseId) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.workout_swap)) }
                        TrazoLink(onClick = { onExercise(ex.exerciseId) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.session_view_exercise)) }
                    }
                }
            }
        }
        state.rest?.let { RestBar(it, viewModel::adjustRest, viewModel::skipRest) }
        TrazoButton(onClick = { showFinish = true }, modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = MinTouch)) { Text(stringResource(R.string.workout_finish)) }
    }

    if (confirmLeave) AlertDialog(
        onDismissRequest = { confirmLeave = false },
        title = { Text(stringResource(R.string.workout_leave_title)) },
        text = { Text(stringResource(R.string.workout_leave_text)) },
        confirmButton = { TextButton(onClick = { confirmLeave = false; onExit() }) { Text(stringResource(R.string.workout_leave)) } },
        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.cancel)) } }
    )
    state.alternatives?.let { AlternativesDialog(it, state.swapError, viewModel) }
    if (showFinish) FinishDialog(state, onDismiss = { showFinish = false; viewModel.dismissFinishError() }, onSend = viewModel::finish)
}

@Composable
private fun SetRow(number: Int, set: SetEntry, onChange: ((SetEntry) -> SetEntry) -> Unit, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.workout_set, number), Modifier.width(52.dp), style = MaterialTheme.typography.bodySmall)
        TrazoField(set.weight, { v -> onChange { it.copy(weight = v) } }, Modifier.weight(1.2f), { Text(stringResource(R.string.workout_weight), maxLines = 1) }, KeyboardOptions(keyboardType = KeyboardType.Decimal))
        TrazoField(set.reps, { v -> onChange { it.copy(reps = v.filter(Char::isDigit).take(3)) } }, Modifier.weight(0.9f), { Text(stringResource(R.string.workout_reps), maxLines = 1) }, KeyboardOptions(keyboardType = KeyboardType.Number))
        TrazoField(set.rir, { v -> onChange { it.copy(rir = v) } }, Modifier.weight(0.8f), { Text(stringResource(R.string.workout_rir), maxLines = 1) }, KeyboardOptions(keyboardType = KeyboardType.Decimal))
        val label = stringResource(if (set.done) R.string.workout_set_done else R.string.workout_mark_set, number)
        FilledIconToggleButton(
            checked = set.done, onCheckedChange = { onToggle() },
            modifier = Modifier.size(MinTouch).semantics { contentDescription = label },
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = TrazoPalette.SecondaryButton, contentColor = TrazoPalette.TextSecondary,
                checkedContainerColor = TrazoPalette.Ok, checkedContentColor = TrazoPalette.Background
            )
        ) { Icon(Icons.Filled.Check, contentDescription = null) }
    }
}

@Composable
private fun RestBar(rest: RestTimer, onAdjust: (Int) -> Unit, onSkip: () -> Unit) {
    Surface(color = TrazoPalette.Sidebar, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.workout_rest), Modifier.weight(1f), color = TrazoPalette.TextSecondary)
                Text(
                    Labels.clock(rest.remainingSeconds), style = MaterialTheme.typography.headlineMedium,
                    color = if (rest.remainingSeconds == 0) TrazoPalette.Ok else TrazoPalette.Text
                )
            }
            LinearProgressIndicator(
                progress = { if (rest.totalSeconds == 0) 0f else 1f - rest.remainingSeconds / rest.totalSeconds.toFloat() },
                modifier = Modifier.fillMaxWidth(), color = TrazoPalette.Link, trackColor = TrazoPalette.Border
            )
            Row {
                TrazoLink(onClick = { onAdjust(-15) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.workout_rest_minus)) }
                TrazoLink(onClick = { onAdjust(15) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.workout_rest_plus)) }
                Spacer(Modifier.weight(1f))
                TrazoLink(onClick = onSkip, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.workout_rest_skip)) }
            }
        }
    }
}

@Composable
private fun AlternativesDialog(state: AlternativesState, swapError: com.trazo.android.data.net.ApiError?, vm: WorkoutViewModel) {
    var remaining by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = vm::dismissAlternatives,
        title = { Text(stringResource(R.string.workout_alternatives_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    state.loading -> LoadingBox(Modifier.height(100.dp))
                    state.error != null -> InlineError(state.error)
                    state.items.isEmpty() -> Text(state.message ?: stringResource(R.string.workout_alternatives_empty))
                    else -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = remaining, onCheckedChange = { remaining = it })
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.workout_swap_block), style = MaterialTheme.typography.bodySmall)
                        }
                        state.items.forEach { alt ->
                            TrazoCard(onClick = { vm.swap(state.exerciseId, alt.exerciseId, remaining) }) {
                                Text(alt.name, style = MaterialTheme.typography.titleSmall)
                                Text(Labels.muscles(alt.primaryMuscles) + " · " + Labels.pattern(alt.movementPattern), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                swapError?.let { InlineError(it) }
            }
        },
        confirmButton = { TextButton(onClick = vm::dismissAlternatives) { Text(stringResource(R.string.close)) } }
    )
}

private val RECOVERIES = listOf("Unknown", "Good", "Partial", "Insufficient")
private val REASONS = listOf("Time", "Fatigue", "Equipment", "Motivation", "Illness", "Other")

@Composable
private fun FinishDialog(state: WorkoutState, onDismiss: () -> Unit, onSend: (FinishForm) -> Unit) {
    var recovery by remember { mutableStateOf("Unknown") }
    var rpe by remember { mutableStateOf<Float?>(null) }
    var pain by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Fatigue") }
    val incomplete = state.doneSets < state.allSets
    val sending = state.finish is FinishState.Sending
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text(stringResource(R.string.finish_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.workout_progress, state.doneSets, state.allSets), color = TrazoPalette.TextSecondary)
                if (incomplete) {
                    Text(stringResource(R.string.finish_reason))
                    ChipGroup(REASONS, setOf(reason), Labels::reason, { reason = it })
                }
                Text(stringResource(R.string.finish_recovery))
                ChipGroup(RECOVERIES, setOf(recovery), Labels::recovery, { recovery = it })
                Text(stringResource(R.string.finish_rpe, rpe?.toInt()?.toString() ?: stringResource(R.string.finish_rpe_unset)))
                Slider(value = rpe ?: 5f, onValueChange = { rpe = it.toInt().toFloat() }, valueRange = 0f..10f, steps = 9, modifier = Modifier.heightIn(min = MinTouch))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = pain, onCheckedChange = { pain = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.finish_pain))
                }
                OutlinedTextField(notes, { notes = it.take(1000) }, label = { Text(stringResource(R.string.finish_notes)) }, modifier = Modifier.fillMaxWidth())
                (state.finish as? FinishState.Failed)?.let { InlineError(it.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !sending, modifier = Modifier.heightIn(min = MinTouch),
                onClick = { onSend(FinishForm(recovery, rpe?.toDouble(), pain, notes, if (incomplete) reason else "None")) }
            ) { Text(stringResource(if (sending) R.string.finish_sending else R.string.finish_send)) }
        },
        dismissButton = { TextButton(enabled = !sending, onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun FinishedScreen(done: FinishState.Done, onExit: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(R.string.finish_sent))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (done.queued) Notice(stringResource(R.string.finish_queued))
            val report = done.result?.adaptation
            if (report != null) {
                SectionTitle(stringResource(R.string.finish_adaptation))
                if (report.sessionExplanation.isNotBlank()) Text(report.sessionExplanation)
                report.exercises.forEach { advice ->
                    TrazoCard {
                        Text(advice.explanation)
                        advice.nextLoadKg?.let { Text(stringResource(R.string.session_target_load, WorkoutViewModel.formatKg(it)), color = TrazoPalette.Link) }
                    }
                }
                if (report.applied.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.finish_applied))
                    report.applied.forEach { Text("• ${it.explanation}") }
                }
                if (report.notes.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.finish_notes_title))
                    report.notes.forEach { Notice(it) }
                }
            }
        }
        TrazoButton(onClick = onExit, modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = MinTouch)) { Text(stringResource(R.string.finish_back_to_plan)) }
    }
}
