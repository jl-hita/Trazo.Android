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
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoField
import com.trazo.android.ui.theme.TrazoPalette

private val WEEK = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

private fun name(item: ExerciseListItem) = if (Labels.english()) item.nameEn.ifBlank { item.nameEs } else item.nameEs.ifBlank { item.nameEn }

@Composable
fun ManualPlanScreen(viewModel: ManualPlanViewModel, onBack: () -> Unit, onCreated: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.created) { if (state.created) onCreated() }
    val order = Labels.inWeekOrder(WEEK)
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(R.string.manual_title), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TrazoField(state.name, viewModel::setName, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.manual_name)) })

            Text(stringResource(R.string.manual_weeks, state.totalWeeks), modifier = Modifier.padding(top = 8.dp))
            Slider(
                value = state.totalWeeks.toFloat(), onValueChange = { viewModel.setTotalWeeks(it.toInt()) },
                valueRange = ManualPlanViewModel.MIN_WEEKS.toFloat()..ManualPlanViewModel.MAX_WEEKS.toFloat(),
                modifier = Modifier.heightIn(min = MinTouch)
            )

            SectionTitle(stringResource(R.string.manual_days))
            Text(stringResource(R.string.manual_days_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
            DayPicker(WEEK, state.days.keys, { day -> viewModel.toggleDay(day, context.getString(R.string.manual_default_routine, state.days.size + 1)) })

            order.filter { it in state.days }.forEach { day ->
                DayCard(day, state.days.getValue(day), viewModel)
            }

            state.error?.let { InlineError(it, Modifier.padding(top = 8.dp)) }
        }
        TrazoButton(
            onClick = { viewModel.submit(order) }, enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = MinTouch)
        ) { Text(stringResource(if (state.submitting) R.string.manual_creating else R.string.manual_create)) }
    }
    if (state.picking != null) ExercisePicker(state, viewModel)
}

@Composable
private fun DayCard(day: String, routine: ManualDay, vm: ManualPlanViewModel) {
    TrazoCard(Modifier.fillMaxWidth()) {
        Text(Labels.day(day), style = MaterialTheme.typography.titleSmall)
        TrazoField(routine.title, { vm.setTitle(day, it) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text(stringResource(R.string.manual_routine_title)) })
        if (routine.exercises.isEmpty()) {
            Text(stringResource(R.string.manual_no_exercises), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
        routine.exercises.forEachIndexed { index, exercise ->
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(exercise.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { vm.removeExercise(day, exercise.id) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.manual_remove)) }
                }
                Stepper(stringResource(R.string.manual_sets, exercise.sets), { vm.changeSets(day, exercise.id, -1) }, { vm.changeSets(day, exercise.id, 1) })
                Stepper(stringResource(R.string.manual_reps, exercise.minReps, exercise.maxReps), { vm.changeReps(day, exercise.id, -1) }, { vm.changeReps(day, exercise.id, 1) })
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
