package com.trazo.android.ui.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.android.R
import com.trazo.android.data.model.ExerciseView
import com.trazo.android.data.model.WeekView
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoLink
import com.trazo.android.ui.theme.TrazoPalette
import com.trazo.android.ui.workout.WorkoutViewModel
import kotlinx.coroutines.flow.StateFlow

/** The prescription of one session: sets, repetitions, RIR, rest and load target for each exercise. */
@Composable
fun SessionScreen(
    week: StateFlow<WeekView?>,
    sessionId: String,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onExercise: (String) -> Unit
) {
    SecureScreen()
    val current by week.collectAsStateWithLifecycle()
    val session = current?.sessions?.firstOrNull { it.id == sessionId }
    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(session?.title ?: stringResource(R.string.workout_title), onBack)
        if (session == null) { EmptyBox(stringResource(R.string.err_not_found)); return@Column }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(
                    "${Labels.day(session.day)} · ${formatDate(session.date)} · " + stringResource(R.string.plan_session_minutes, session.estimatedDurationMinutes),
                    color = TrazoPalette.TextSecondary
                )
            }
            items(session.exercises, key = { it.exerciseId }) { ExercisePrescription(it, onExercise) }
        }
        if (session.log == null) {
            TrazoButton(onClick = onStart, modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = MinTouch)) { Text(stringResource(R.string.session_start)) }
        } else {
            Text(stringResource(R.string.session_done), color = TrazoPalette.Ok, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
fun ExercisePrescription(ex: ExerciseView, onExercise: (String) -> Unit) {
    TrazoCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            ExerciseThumb(exerciseImageUrl(ex.exerciseId), ex.name, 56.dp)
            Column {
                Text(ex.name, style = MaterialTheme.typography.titleMedium)
                Text(Labels.muscles(ex.primaryMuscles), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        val parts = buildList {
            add(stringResource(R.string.session_sets_reps, ex.sets, ex.minRepetitions, ex.maxRepetitions))
            ex.targetRir?.let { add(stringResource(R.string.session_rir, WorkoutViewModel.formatKg(it))) }
            add(stringResource(R.string.session_rest, Labels.clock(ex.restSeconds)))
        }
        Text(parts.joinToString(" · "))
        ex.targetLoadKg?.let { Text(stringResource(R.string.session_target_load, WorkoutViewModel.formatKg(it)), color = TrazoPalette.Link) }
        ex.warmupSets?.takeIf { it > 0 }?.let {
            Text(
                stringResource(R.string.session_warmup, it) + (ex.warmupNote?.takeIf(String::isNotBlank)?.let { n -> " — $n" } ?: ""),
                color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall
            )
        }
        if (ex.swappedFromExerciseId != null) Text(stringResource(R.string.session_swapped), color = TrazoPalette.Muted, style = MaterialTheme.typography.bodySmall)
        TrazoLink(onClick = { onExercise(ex.exerciseId) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.session_view_exercise)) }
    }
}
