package com.trazo.android.ui.manual

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.android.R
import com.trazo.android.data.model.ExerciseListItem
import com.trazo.android.data.model.Techniques
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoField
import com.trazo.android.ui.theme.TrazoPalette

private val WEEK = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

private fun name(item: ExerciseListItem) = Labels.itemName(item)

@Composable
fun ManualPlanScreen(viewModel: ManualPlanViewModel, onBack: () -> Unit, onCreated: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.created) { if (state.created) onCreated() }
    val order = Labels.inWeekOrder(WEEK)
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(if (state.editing) R.string.manual_edit_title else R.string.manual_title), onBack)
        if (state.loadingExisting) { LoadingBox(Modifier.weight(1f)); return@Column }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TrazoField(state.name, viewModel::setName, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.manual_name)) })

            if (state.editing) Notice(stringResource(R.string.manual_edit_note))
            if (!state.editing) Text(stringResource(R.string.manual_weeks, state.totalWeeks), modifier = Modifier.padding(top = 8.dp))
            if (!state.editing) Slider(
                value = state.totalWeeks.toFloat(), onValueChange = { viewModel.setTotalWeeks(it.toInt()) },
                valueRange = ManualPlanViewModel.MIN_WEEKS.toFloat()..ManualPlanViewModel.MAX_WEEKS.toFloat(),
                modifier = Modifier.heightIn(min = MinTouch)
            )

            // The way of scheduling is chosen when creating, and only when the server offers the sequence mode.
            if (!state.editing && state.sequenceAvailable) {
                SectionTitle(stringResource(R.string.manual_mode))
                ChipGroup(
                    listOf("Calendar", "Sequence"), setOf(state.schedulingMode),
                    { context.getString(if (it == "Sequence") R.string.onb_mode_sequence else R.string.onb_mode_calendar) },
                    viewModel::setSchedulingMode
                )
            }

            if (state.isSequence) {
                SectionTitle(stringResource(R.string.manual_routines))
                Text(stringResource(R.string.manual_routines_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                val keys = state.order.filter { it in state.days }
                keys.forEachIndexed { index, key ->
                    DayCard(
                        stringResource(R.string.manual_default_routine, index + 1), key, state.days.getValue(key), viewModel,
                        actions = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { viewModel.moveRoutine(key, -1) }, enabled = index > 0, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.manual_move_up)) }
                                TextButton(onClick = { viewModel.moveRoutine(key, 1) }, enabled = index < keys.lastIndex, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.manual_move_down)) }
                                TextButton(onClick = { viewModel.removeRoutine(key) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.manual_remove_routine)) }
                            }
                        }
                    )
                }
                if (keys.size < ManualPlanViewModel.MAX_ROUTINES) TrazoButton(
                    onClick = { viewModel.addRoutine(context.getString(R.string.manual_default_routine, keys.size + 1)) }, secondary = true,
                    modifier = Modifier.heightIn(min = MinTouch)
                ) { Text(stringResource(R.string.manual_add_routine)) }
            } else {
                SectionTitle(stringResource(R.string.manual_days))
                Text(stringResource(R.string.manual_days_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                DayPicker(WEEK, state.days.keys, { day -> viewModel.toggleDay(day, context.getString(R.string.manual_default_routine, state.days.size + 1)) })

                order.filter { it in state.days }.forEach { day ->
                    DayCard(Labels.day(day), day, state.days.getValue(day), viewModel)
                }
            }

            state.error?.let { InlineError(it, Modifier.padding(top = 8.dp)) }
        }
        TrazoButton(
            onClick = { viewModel.submit(order) }, enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = MinTouch)
        ) { Text(stringResource(if (state.editing) (if (state.submitting) R.string.manual_saving else R.string.manual_save) else if (state.submitting) R.string.manual_creating else R.string.manual_create)) }
    }
    if (state.picking != null) ExercisePicker(state, viewModel)
    state.technique?.let { pick ->
        state.days[pick.day]?.exercises?.firstOrNull { it.id == pick.exerciseId }?.let { TechniqueDialog(pick, it, viewModel) }
    }
}

@Composable
private fun TechniqueDialog(pick: TechniquePick, exercise: ManualExercise, vm: ManualPlanViewModel) {
    val straight = stringResource(R.string.technique_straight)
    // A match needs two sets; what the exercise already has stays selectable.
    val choices = pick.allowed.filter { it != Techniques.MATCH || exercise.sets > 1 }
    val names = choices.associateWith { techniqueName(it) }
    val scopes = mapOf(
        Techniques.LAST_SET to stringResource(R.string.technique_scope_last), Techniques.ALL_SETS to stringResource(R.string.technique_scope_all)
    )
    AlertDialog(
        onDismissRequest = vm::closeTechnique,
        title = { Text(exercise.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val error = pick.error
                when {
                    pick.loading -> LoadingBox(Modifier.height(100.dp))
                    error != null -> InlineError(error)
                    choices.isEmpty() -> Text(stringResource(R.string.technique_none_allowed))
                    else -> {
                        ChipGroup(
                            listOf("") + choices, setOf(exercise.technique?.type.orEmpty()), { names[it] ?: straight },
                            { vm.setTechnique(it.ifEmpty { null }) }
                        )
                        exercise.technique?.let { technique ->
                            if (technique.type == Techniques.DROP_SET) Stepper(
                                stringResource(R.string.technique_drops, technique.maxSegments), { vm.changeDrops(-1) }, { vm.changeDrops(1) }
                            )
                            if (technique.type != Techniques.MATCH) ChipGroup(
                                scopes.keys.toList(), setOf(technique.appliesTo), { scopes.getValue(it) }, vm::setTechniqueScope
                            )
                            Text(techniqueHelp(technique), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = vm::closeTechnique) { Text(stringResource(R.string.manual_close)) } }
    )
}

@Composable
private fun DayCard(header: String, day: String, routine: ManualDay, vm: ManualPlanViewModel, actions: @Composable () -> Unit = {}) {
    TrazoCard(Modifier.fillMaxWidth()) {
        Text(header, style = MaterialTheme.typography.titleSmall)
        actions()
        TrazoField(routine.title, { vm.setTitle(day, it) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text(stringResource(R.string.manual_routine_title)) })
        if (routine.exercises.isEmpty()) {
            Text(stringResource(R.string.manual_no_exercises), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
        val groups = ManualPlanViewModel.supersetGroups(routine.exercises)
        routine.exercises.forEachIndexed { index, exercise ->
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                // The rounds and rests of a superset are set once, above its first exercise.
                val group = groups[index]
                if (group != null && (index == 0 || groups[index - 1] != group)) {
                    val superset = ManualPlanViewModel.supersetOf(routine.exercises.filterIndexed { i, _ -> groups[i] == group })
                    val step = ManualPlanViewModel.REST_STEP
                    Text(stringResource(R.string.manual_superset_title, group), color = TrazoPalette.Link, style = MaterialTheme.typography.titleSmall)
                    Stepper(stringResource(R.string.workout_superset_rounds, superset.rounds), { vm.changeRounds(day, exercise.id, -1) }, { vm.changeRounds(day, exercise.id, 1) })
                    Stepper(
                        stringResource(R.string.manual_rest_between, superset.restBetweenSeconds),
                        { vm.changeSupersetRest(day, exercise.id, false, -step) }, { vm.changeSupersetRest(day, exercise.id, false, step) }
                    )
                    Stepper(
                        stringResource(R.string.manual_rest_after, superset.restAfterRoundSeconds),
                        { vm.changeSupersetRest(day, exercise.id, true, -step) }, { vm.changeSupersetRest(day, exercise.id, true, step) }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(exercise.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { vm.removeExercise(day, exercise.id) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.manual_remove)) }
                }
                Stepper(stringResource(R.string.manual_sets, exercise.sets), { vm.changeSets(day, exercise.id, -1) }, { vm.changeSets(day, exercise.id, 1) })
                Stepper(stringResource(R.string.manual_reps, exercise.minReps, exercise.maxReps), { vm.changeReps(day, exercise.id, -1) }, { vm.changeReps(day, exercise.id, 1) })
                // A superset is not combined with another technique.
                val joined = exercise.linkedToNext || routine.exercises.getOrNull(index - 1)?.linkedToNext == true
                if (!joined) TextButton(onClick = { vm.openTechnique(day, exercise.id) }, modifier = Modifier.heightIn(min = MinTouch)) {
                    Text(stringResource(R.string.technique_label, exercise.technique?.let { techniqueName(it.type, it.maxSegments) } ?: stringResource(R.string.technique_straight)))
                }
                if (index < routine.exercises.lastIndex) TextButton(onClick = { vm.toggleLink(day, exercise.id) }, modifier = Modifier.heightIn(min = MinTouch)) {
                    Text(stringResource(if (exercise.linkedToNext) R.string.manual_unlink else R.string.manual_link))
                }
            }
        }
        TrazoButton(onClick = { vm.openPicker(day) }, secondary = true, modifier = Modifier.padding(top = 12.dp).heightIn(min = MinTouch)) {
            Text(stringResource(R.string.manual_add_exercise))
        }
    }
}

@Composable
private fun Stepper(label: String, onLess: () -> Unit, onMore: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = TrazoPalette.TextSecondary)
        OutlinedButton(onClick = onLess, modifier = Modifier.heightIn(min = MinTouch)) { Text("−") }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = onMore, modifier = Modifier.heightIn(min = MinTouch)) { Text("+") }
    }
}

@Composable
private fun ExercisePicker(state: ManualPlanState, vm: ManualPlanViewModel) {
    AlertDialog(
        onDismissRequest = vm::closePicker,
        title = { Text(stringResource(R.string.manual_add_exercise)) },
        text = {
            Column {
                TrazoField(state.query, vm::search, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.manual_search)) })
                when {
                    state.searching && state.results.isEmpty() -> LoadingBox(Modifier.height(120.dp))
                    state.results.isEmpty() -> Text(stringResource(R.string.manual_search_empty), color = TrazoPalette.TextSecondary, modifier = Modifier.padding(top = 12.dp))
                    else -> LazyColumn(Modifier.heightIn(max = 320.dp).padding(top = 8.dp)) {
                        items(state.results, key = { it.id }) { item ->
                            TextButton(onClick = { vm.pick(item, name(item)) }, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) {
                                Text(name(item), Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = vm::closePicker) { Text(stringResource(R.string.manual_close)) } }
    )
}
