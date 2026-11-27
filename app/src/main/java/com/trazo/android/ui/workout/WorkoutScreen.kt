package com.trazo.android.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import com.trazo.android.data.model.AlternativeView
import com.trazo.android.data.model.ExerciseFeedbackRequest
import com.trazo.android.data.model.LastPerformanceView
import com.trazo.android.ui.plan.formatDate
import com.trazo.android.ui.catalog.ExerciseInfoDialog
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoField
import com.trazo.android.ui.theme.TrazoLink
import com.trazo.android.ui.theme.TrazoPalette

@Composable
fun WorkoutScreen(viewModel: WorkoutViewModel, onExit: () -> Unit, onExercise: (String) -> Unit, onRestStarted: () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resting = state.rest != null
    LaunchedEffect(resting) { if (resting) onRestStarted() }
    var confirmLeave by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }
    // The exercise whose explanation is open (steps and photos), reachable from the photo and from the swap picker.
    var info by remember { mutableStateOf<String?>(null) }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        val explain = stringResource(R.string.exercise_explain)
                        ExerciseThumb(
                            exerciseImageUrl(entry.exercise.exerciseId), entry.exercise.name, 56.dp,
                            Modifier.clickable(onClickLabel = explain) { info = entry.exercise.exerciseId }
                        )
                        Column {
                            Text(entry.exercise.name, style = MaterialTheme.typography.titleMedium)
                            if (entry.exercise.isExtra) Text(stringResource(R.string.workout_extra_tag), color = TrazoPalette.Link, style = MaterialTheme.typography.labelMedium)
                            entry.exercise.supersetGroup?.let { group ->
                                Text(stringResource(R.string.workout_superset, group), color = TrazoPalette.Link, style = MaterialTheme.typography.labelMedium)
                                state.session?.supersets?.firstOrNull { it.group == group }?.let {
                                    Text(stringResource(R.string.workout_superset_rounds, it.rounds), color = TrazoPalette.Link, style = MaterialTheme.typography.labelMedium)
                                    techniqueReason(it.reason)?.let { why -> Text(why, color = TrazoPalette.Muted, style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                        }
                    }
                    val ex = entry.exercise
                    Text(
                        stringResource(R.string.session_sets_reps, ex.sets, ex.minRepetitions, ex.maxRepetitions) +
                            (ex.targetRir?.let { " · " + stringResource(R.string.session_rir, WorkoutViewModel.formatKg(it)) } ?: "") +
                            (ex.targetLoadKg?.let { " · " + stringResource(R.string.session_target_load, WorkoutViewModel.formatKg(it)) } ?: ""),
                        color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall
                    )
                    ex.lastPerformance?.let { LastTimeInfo(it, ex.targetLoadKg == null) }
                    ex.technique?.let {
                        Text(techniqueSummary(it), color = TrazoPalette.Link, style = MaterialTheme.typography.labelMedium)
                        Text(techniqueHelp(it), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        techniqueReason(it.reason)?.let { why -> Text(why, color = TrazoPalette.Muted, style = MaterialTheme.typography.bodySmall) }
                    }
                    entry.sets.forEachIndexed { index, set ->
                        SetRow(
                            stringResource(R.string.workout_set, index + 1), stringResource(R.string.workout_mark_set, index + 1),
                            stringResource(R.string.workout_set_done, index + 1), set,
                            onChange = { t -> viewModel.updateSet(ex.exerciseId, index, t) },
                            onToggle = { viewModel.toggleDone(ex.exerciseId, index) }
                        )
                        val technique = WorkoutViewModel.carried(entry, index)
                        // Segments appear once their set is done, and stay out of the way if it is unmarked.
                        if (technique != null && set.done) {
                            set.segments.forEachIndexed { k, segment ->
                                val label = stringResource(R.string.workout_segment, k + 1)
                                SetRow(
                                    label, label, stringResource(R.string.workout_segment_done, label), segment,
                                    onChange = { t -> viewModel.updateSegment(ex.exerciseId, index, k, t) },
                                    onToggle = { viewModel.toggleSegment(ex.exerciseId, index, k) }
                                )
                            }
                            if (set.segments.any { !it.done }) TrazoLink(onClick = { viewModel.finishSet(ex.exerciseId, index) }, modifier = Modifier.heightIn(min = MinTouch)) {
                                Text(stringResource(R.string.workout_finish_set))
                            }
                            else if (set.segments.size < technique.maxSegments) TrazoLink(onClick = { viewModel.addSegment(ex.exerciseId, index) }, modifier = Modifier.heightIn(min = MinTouch)) {
                                Text(stringResource(R.string.workout_add_segment))
                            }
                        }
                    }
                    Row {
                        TrazoLink(onClick = { viewModel.showAlternatives(ex.exerciseId) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.workout_swap)) }
                        TrazoLink(onClick = { onExercise(ex.exerciseId) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.session_view_exercise)) }
                    }
                }
            }
        }
        TrazoLink(onClick = viewModel::openAddExercise, modifier = Modifier.padding(horizontal = 16.dp).heightIn(min = MinTouch)) { Text(stringResource(R.string.workout_add_exercise)) }
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
    state.alternatives?.let { AlternativesDialog(it, state.swapError, viewModel::swap, viewModel::dismissAlternatives, onInfo = { info = it }) }
    info?.let { id -> viewModel.catalog?.let { ExerciseInfoDialog(id, it, onDismiss = { info = null }) } }
    state.adding?.let { AddExerciseDialog(it, viewModel) }
    if (showFinish) FinishDialog(state, onDismiss = { showFinish = false; viewModel.dismissFinishError() }, onSend = viewModel::finish)
}

/** What the user did last time and, when the engine gave no target, the load proposed for today. */
@Composable
private fun LastTimeInfo(last: LastPerformanceView, showProposal: Boolean) {
    val sets = last.sets.filter { it.repetitions != null }.joinToString(" · ") { set ->
        (set.weightKg?.let { WorkoutViewModel.formatKg(it) + " kg" } ?: "–") + " × " + set.repetitions
    }
    if (sets.isNotEmpty()) Text(
        stringResource(R.string.workout_last_time, formatDate(last.date), sets),
        color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall
    )
    val proposed = last.suggestedLoadKg
    if (showProposal && proposed != null) Text(
        when (last.suggestionBasis) {
            "increase" -> stringResource(R.string.workout_proposal_increase, WorkoutViewModel.formatKg(proposed))
            "estimate" -> stringResource(R.string.workout_proposal_estimate, WorkoutViewModel.formatKg(proposed), last.basedOnSessions ?: 2)
            else -> stringResource(R.string.workout_proposal_repeat, WorkoutViewModel.formatKg(proposed))
        },
        color = TrazoPalette.Link, style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun SetRow(title: String, markLabel: String, doneLabel: String, set: SetEntry, onChange: ((SetEntry) -> SetEntry) -> Unit, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, Modifier.width(52.dp), style = MaterialTheme.typography.bodySmall)
        TrazoField(set.weight, { v -> onChange { it.copy(weight = v) } }, Modifier.weight(1.2f), { Text(stringResource(R.string.workout_weight), maxLines = 1) }, KeyboardOptions(keyboardType = KeyboardType.Decimal))
        TrazoField(set.reps, { v -> onChange { it.copy(reps = v.filter(Char::isDigit).take(3)) } }, Modifier.weight(0.9f), { Text(stringResource(R.string.workout_reps), maxLines = 1) }, KeyboardOptions(keyboardType = KeyboardType.Number))
        TrazoField(set.rir, { v -> onChange { it.copy(rir = v) } }, Modifier.weight(0.8f), { Text(stringResource(R.string.workout_rir), maxLines = 1) }, KeyboardOptions(keyboardType = KeyboardType.Decimal))
        val label = if (set.done) doneLabel else markLabel
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

/** Picks any exercise of the catalog to do in this session only: it is not repeated in the next ones. */
@Composable
private fun AddExerciseDialog(state: AddExerciseState, vm: WorkoutViewModel) {
    AlertDialog(
        onDismissRequest = vm::closeAddExercise,
        title = { Text(stringResource(R.string.workout_add_exercise)) },
        text = {
            Column {
                Text(stringResource(R.string.workout_add_exercise_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                TrazoField(state.query, vm::searchToAdd, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text(stringResource(R.string.manual_search)) })
                when {
                    state.searching && state.results.isEmpty() -> LoadingBox(Modifier.height(120.dp))
                    state.results.isEmpty() -> Text(stringResource(R.string.manual_search_empty), color = TrazoPalette.TextSecondary, modifier = Modifier.padding(top = 12.dp))
                    else -> LazyColumn(Modifier.heightIn(max = 320.dp).padding(top = 8.dp)) {
                        items(state.results, key = { it.id }) { item ->
                            TextButton(onClick = { vm.addExercise(item) }, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) {
                                Text(Labels.itemName(item), Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                state.error?.let { InlineError(it) }
            }
        },
        confirmButton = { TextButton(onClick = vm::closeAddExercise) { Text(stringResource(R.string.close)) } }
    )
}

/**
 * The replacements the engine proposes for an exercise. Each one can be opened to read how it is done before choosing. Choosing one
 * asks how far the change reaches; nothing is sent until that is answered, and cancelling there closes the whole flow.
 */
@Composable
fun AlternativesDialog(
    state: AlternativesState, swapError: com.trazo.android.data.net.ApiError?,
    onSwap: (exerciseId: String, replacementId: String, remainingBlock: Boolean) -> Unit, onDismiss: () -> Unit, onInfo: (String) -> Unit
) {
    var chosen by remember(state.exerciseId) { mutableStateOf<AlternativeView?>(null) }
    chosen?.let { replacement ->
        SwapScopeDialog(
            replacement.name, state.swapping, swapError,
            onChoose = { remainingBlock -> onSwap(state.exerciseId, replacement.exerciseId, remainingBlock) },
            onCancel = onDismiss
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workout_alternatives_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    state.loading -> LoadingBox(Modifier.height(100.dp))
                    state.error != null -> InlineError(state.error)
                    state.items.isEmpty() -> Text(state.message ?: stringResource(R.string.workout_alternatives_empty))
                    else -> {
                        state.items.forEach { alt ->
                            TrazoCard(onClick = { chosen = alt }) {
                                Text(alt.name, style = MaterialTheme.typography.titleSmall)
                                Text(Labels.muscles(alt.primaryMuscles) + " · " + Labels.pattern(alt.movementPattern), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                                TrazoLink(onClick = { onInfo(alt.exerciseId) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.exercise_see_detail)) }
                            }
                        }
                    }
                }
                swapError?.let { InlineError(it) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

/** Asks whether a replacement is for this session only or also for the pending ones that follow it in the block. */
@Composable
private fun SwapScopeDialog(
    replacement: String, swapping: Boolean, error: com.trazo.android.data.net.ApiError?,
    onChoose: (remainingBlock: Boolean) -> Unit, onCancel: () -> Unit
) {
    AlertDialog(
        // Once a choice is sent it cannot be taken back, so the dialog stays until the server answers.
        onDismissRequest = { if (!swapping) onCancel() },
        title = { Text(stringResource(R.string.workout_swap_scope_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(replacement, style = MaterialTheme.typography.titleSmall)
                TrazoButton(onClick = { onChoose(false) }, enabled = !swapping, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) {
                    Text(stringResource(R.string.workout_swap_scope_session))
                }
                TrazoButton(onClick = { onChoose(true) }, enabled = !swapping, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) {
                    Text(stringResource(R.string.workout_swap_scope_block))
                }
                Text(stringResource(R.string.workout_swap_scope_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                if (swapping) LoadingBox(Modifier.height(60.dp))
                error?.let { InlineError(it) }
            }
        },
        confirmButton = { TextButton(onClick = onCancel, enabled = !swapping) { Text(stringResource(R.string.cancel)) } }
    )
}

private val RECOVERIES = listOf("Unknown", "Good", "Partial", "Insufficient")
private val STIMULUS = listOf("low", "adequate", "high")
private val DISCOMFORT = listOf("none", "mild", "strong")
private val PRE_FATIGUE = listOf("recovered", "loaded")

private fun fbLabel(context: android.content.Context, value: String): String = context.getString(
    when (value) {
        "low" -> R.string.fb_low
        "adequate" -> R.string.fb_adequate
        "high" -> R.string.fb_high
        "none" -> R.string.fb_none
        "mild" -> R.string.fb_mild
        "strong" -> R.string.fb_strong
        "recovered" -> R.string.fb_recovered
        else -> R.string.fb_loaded
    }
)

private val REASONS = listOf("Time", "Fatigue", "Equipment", "Motivation", "Illness", "Other")

@Composable
private fun FinishDialog(state: WorkoutState, onDismiss: () -> Unit, onSend: (FinishForm) -> Unit) {
    var recovery by remember { mutableStateOf("Unknown") }
    var rpe by remember { mutableStateOf<Float?>(null) }
    var pain by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Fatigue") }
    var feedback by remember { mutableStateOf<Map<String, ExerciseFeedbackRequest>>(emptyMap()) }
    var performedOn by remember { mutableStateOf<java.time.LocalDate?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val health = com.trazo.android.ui.common.LocalHealthGate.current
    val incomplete = state.doneSets < state.allSets
    val sending = state.finish is FinishState.Sending
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text(stringResource(R.string.finish_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.workout_progress, state.doneSets, state.allSets), color = TrazoPalette.TextSecondary)
                WhenPicker(performedOn) { performedOn = it }
                if (incomplete) {
                    Text(stringResource(R.string.finish_reason))
                    ChipGroup(REASONS, setOf(reason), Labels::reason, { value -> if (value == "Illness") health { reason = value } else reason = value })
                }
                Text(stringResource(R.string.finish_recovery))
                ChipGroup(RECOVERIES, setOf(recovery), Labels::recovery, { recovery = it })
                Text(stringResource(R.string.finish_rpe, rpe?.toInt()?.toString() ?: stringResource(R.string.finish_rpe_unset)))
                Slider(value = rpe ?: 5f, onValueChange = { rpe = it.toInt().toFloat() }, valueRange = 0f..10f, steps = 9, modifier = Modifier.heightIn(min = MinTouch))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = pain, onCheckedChange = { on -> if (on) health { pain = true } else pain = false })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.finish_pain))
                }
                val answered = state.entries.filter { e -> e.sets.any { it.done } }
                if (answered.isNotEmpty()) {
                    Text(stringResource(R.string.fb_title))
                    Text(stringResource(R.string.fb_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    answered.forEach { e ->
                        val id = e.exercise.exerciseId
                        val current = feedback[id] ?: ExerciseFeedbackRequest()
                        fun update(next: ExerciseFeedbackRequest) { feedback = feedback + (id to next) }
                        Text(e.exercise.name, style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.fb_stimulus), style = MaterialTheme.typography.bodySmall, color = TrazoPalette.TextSecondary)
                        ChipGroup(STIMULUS, setOfNotNull(current.stimulus), { fbLabel(context, it) }, { v -> update(current.copy(stimulus = v.takeUnless { it == current.stimulus })) })
                        Text(stringResource(R.string.fb_discomfort), style = MaterialTheme.typography.bodySmall, color = TrazoPalette.TextSecondary)
                        ChipGroup(DISCOMFORT, setOfNotNull(current.discomfort), { fbLabel(context, it) }, { v ->
                            // Read the answers again when it runs: the consent question may have been on screen meanwhile.
                            fun set(value: String?) { val latest = feedback[id] ?: ExerciseFeedbackRequest(); update(latest.copy(discomfort = value)) }
                            if (v == current.discomfort) set(null) else health { set(v) }
                        })
                        Text(stringResource(R.string.fb_prefatigue), style = MaterialTheme.typography.bodySmall, color = TrazoPalette.TextSecondary)
                        ChipGroup(PRE_FATIGUE, setOfNotNull(current.preFatigue), { fbLabel(context, it) }, { v -> update(current.copy(preFatigue = v.takeUnless { it == current.preFatigue })) })
                    }
                }
                OutlinedTextField(notes, { notes = it.take(1000) }, label = { Text(stringResource(R.string.finish_notes)) }, modifier = Modifier.fillMaxWidth())
                (state.finish as? FinishState.Failed)?.let { InlineError(it.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !sending, modifier = Modifier.heightIn(min = MinTouch),
                onClick = { onSend(FinishForm(recovery, rpe?.toDouble(), pain, notes, if (incomplete) reason else "None", feedback, performedOn)) }
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
                if (report.locked) Notice(stringResource(R.string.finish_adaptation_locked))
                else report.sessionExplanation?.takeIf { it.isNotBlank() }?.let { Text(it) }
                report.exercises.forEach { advice ->
                    TrazoCard {
                        Text(advice.explanation)
                        advice.nextLoadKg?.let { Text(stringResource(R.string.session_target_load, WorkoutViewModel.formatKg(it)), color = TrazoPalette.Link) }
                    }
                }
                if (report.applied.isNotEmpty()) {
                    // The engine recalculated the rest of the block with this session: say it, with what changed.
                    SectionTitle(stringResource(R.string.finish_applied))
                    Text(stringResource(R.string.finish_recalculated, report.applied.size), color = TrazoPalette.Link)
                    report.applied.forEach { Text("• ${it.explanation}") }
                } else if (!report.locked) {
                    Text(stringResource(R.string.finish_reviewed_kept), color = TrazoPalette.TextSecondary)
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

/** Which day the workout happened: today by default, yesterday, or any earlier day. Registering late never changes when it happened. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun WhenPicker(selected: java.time.LocalDate?, onChange: (java.time.LocalDate?) -> Unit) {
    val today = java.time.LocalDate.now()
    var picking by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val key = when (selected) {
        null, today -> "today"
        today.minusDays(1) -> "yesterday"
        else -> "other"
    }
    Text(stringResource(R.string.finish_when))
    ChipGroup(
        listOf("today", "yesterday", "other"), setOf(key),
        { k ->
            when (k) {
                "today" -> context.getString(R.string.finish_when_today)
                "yesterday" -> context.getString(R.string.finish_when_yesterday)
                else -> if (key == "other" && selected != null) formatDate(selected.toString()) else context.getString(R.string.finish_when_other)
            }
        },
        { k ->
            when (k) {
                "today" -> onChange(null)
                "yesterday" -> onChange(today.minusDays(1))
                else -> picking = true
            }
        }
    )
    if (key != "today") Text(stringResource(R.string.finish_when_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
    if (picking) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (selected ?: today).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !java.time.Instant.ofEpochMilli(utcTimeMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate().isAfter(today)
            }
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()) }
                    picking = false
                }) { Text(stringResource(R.string.finish_when_ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state) }
    }
}
