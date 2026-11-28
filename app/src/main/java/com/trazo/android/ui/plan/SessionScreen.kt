package com.trazo.android.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.android.R
import com.trazo.android.data.model.ExerciseView
import com.trazo.android.data.model.LoggedSetView
import com.trazo.android.data.model.SessionView
import com.trazo.android.data.model.SupersetView
import com.trazo.android.data.model.WeekView
import com.trazo.android.ui.catalog.ExerciseInfoDialog
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoLink
import com.trazo.android.ui.theme.TrazoPalette
import com.trazo.android.ui.workout.AlternativesDialog
import com.trazo.android.ui.workout.WorkoutViewModel
import kotlinx.coroutines.flow.StateFlow

/** The prescription of one session: sets, repetitions, RIR, rest and load target for each exercise. */
@Composable
fun SessionScreen(
    viewModel: SessionViewModel,
    week: StateFlow<WeekView?>,
    sessionId: String,
    /** Finds a session the current week does not hold (a late one from an earlier week). */
    lookup: (String) -> SessionView?,
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onStart: () -> Unit,
    onExercise: (String) -> Unit,
    onEdit: () -> Unit = {}
) {
    val current by week.collectAsStateWithLifecycle()
    val ui by viewModel.state.collectAsStateWithLifecycle()
    var info by remember { mutableStateOf<String?>(null) }
    val queued by viewModel.queuedSessions.collectAsStateWithLifecycle()
    LaunchedEffect(sessionId) { viewModel.checkDraft() }
    val session = ui.swapped ?: current?.sessions?.firstOrNull { it.id == sessionId } ?: lookup(sessionId) ?: ui.found
    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(session?.title ?: stringResource(R.string.workout_title), onBack)
        if (session == null) {
            if (ui.searching) LoadingBox() else EmptyBox(stringResource(R.string.session_past_unavailable))
            return@Column
        }
        val logged = session.log != null
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(
                    sessionWhen(session) + " · " + stringResource(R.string.plan_session_minutes, session.estimatedDurationMinutes),
                    color = TrazoPalette.TextSecondary
                )
            }
            if (ui.generated && !logged && session.status == "Planned" && current?.locked != true) item {
                if (session.customised) Text(stringResource(R.string.session_customised), color = TrazoPalette.Muted, style = MaterialTheme.typography.bodySmall)
                TrazoLink(onClick = { viewModel.forgetSwapped(); onEdit() }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.session_edit)) }
            }
            itemsIndexed(session.exercises, key = { _, it -> it.exerciseId }) { index, ex ->
                ExercisePrescription(
                    ex, onExercise,
                    supersetText = ex.supersetGroup?.let { supersetHint(it, session.exercises.getOrNull(index + 1)?.supersetGroup, session.supersets) },
                    // A session already done keeps its exercises: what was done is shown instead of a swap.
                    onSwap = if (logged || current?.locked == true) null else ({ viewModel.showAlternatives(ex.exerciseId) }),
                    onInfo = { info = ex.exerciseId },
                    done = session.log?.sets?.filter { it.exerciseId == ex.exerciseId }
                )
            }
        }
        if (current?.locked == true) {
            Column(Modifier.padding(16.dp)) {
                Notice(stringResource(R.string.plan_generated_premium_locked))
                TrazoButton(onClick = onPremium, modifier = Modifier.padding(top = 8.dp).heightIn(min = MinTouch)) { Text(stringResource(R.string.premium_get)) }
            }
        } else if (session.log == null && sessionId in queued) {
            // Already trained and waiting to be sent: starting it again would record it twice.
            Text(stringResource(R.string.finish_queued), color = TrazoPalette.Ok, modifier = Modifier.padding(16.dp))
        } else if (session.log == null) {
            TrazoButton(onClick = onStart, modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = MinTouch)) {
                Text(stringResource(if (ui.started) R.string.session_resume else R.string.session_start))
            }
        } else {
            Text(stringResource(R.string.session_done), color = TrazoPalette.Ok, modifier = Modifier.padding(16.dp))
        }
    }
    ui.alternatives?.let { AlternativesDialog(it, ui.swapError, viewModel::swap, viewModel::dismissAlternatives, onInfo = { id -> info = id }) }
    info?.let { id -> ExerciseInfoDialog(id, viewModel.catalog, onDismiss = { info = null }) }
}

/** What to do after a set of a superset exercise: go on to the next one or, after the last, rest and start the round again. */
@Composable
fun supersetHint(group: String, nextGroup: String?, supersets: List<SupersetView>?): String = stringResource(
    when {
        nextGroup != group -> R.string.workout_superset_last
        (supersets?.firstOrNull { it.group == group }?.restBetweenSeconds ?: 0) > 0 -> R.string.workout_superset_short_rest
        else -> R.string.workout_superset
    },
    group
)

@Composable
fun ExercisePrescription(
    ex: ExerciseView, onExercise: (String) -> Unit, onSwap: (() -> Unit)? = null, onInfo: (() -> Unit)? = null,
    /** The sets logged for this exercise when the session is done (weight, repetitions, reserve). */
    done: List<LoggedSetView>? = null,
    /** What the superset asks after a set of this exercise. */
    supersetText: String? = null
) {
    TrazoCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            val explain = stringResource(R.string.exercise_explain)
            ExerciseThumb(
                exerciseImageUrl(ex.exerciseId), ex.name, 56.dp,
                if (onInfo != null) Modifier.clickable(onClickLabel = explain, onClick = onInfo) else Modifier
            )
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
        supersetText?.let { Text(it, color = TrazoPalette.Link, style = MaterialTheme.typography.bodySmall) }
        ex.technique?.let {
            Text(techniqueSummary(it), color = TrazoPalette.Link)
            Text(techniqueHelp(it), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
            techniqueReason(it.reason)?.let { why -> Text(why, color = TrazoPalette.Muted, style = MaterialTheme.typography.bodySmall) }
        }
        ex.targetLoadKg?.let { Text(stringResource(R.string.session_target_load, WorkoutViewModel.formatKg(it)), color = TrazoPalette.Link) }
        ex.warmupSets?.takeIf { it > 0 }?.let {
            Text(
                stringResource(R.string.session_warmup, it) + (ex.warmupNote?.takeIf(String::isNotBlank)?.let { n -> " — $n" } ?: ""),
                color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall
            )
        }
        if (ex.swappedFromExerciseId != null) Text(stringResource(R.string.session_swapped), color = TrazoPalette.Muted, style = MaterialTheme.typography.bodySmall)
        if (!done.isNullOrEmpty()) {
            SectionTitle(stringResource(R.string.session_done_sets))
            done.sortedWith(compareBy({ it.setNumber }, { it.segment })).forEach { set ->
                val weight = set.weightKg?.let { WorkoutViewModel.formatKg(it) + " kg" } ?: "–"
                val repetitions = set.repetitions?.toString() ?: "–"
                Text(
                    (if (set.segment > 0) stringResource(R.string.session_done_segment, set.segment, weight, repetitions)
                    else stringResource(R.string.session_done_set, set.setNumber, weight, repetitions)) +
                        (set.rir?.let { " · " + stringResource(R.string.session_rir, WorkoutViewModel.formatKg(it)) } ?: "") +
                        (if (!set.completed) " · " + stringResource(R.string.session_set_skipped) else ""),
                    color = if (set.completed) TrazoPalette.Text else TrazoPalette.TextSecondary
                )
            }
        }
        Row {
            if (onSwap != null) TrazoLink(onClick = onSwap, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.workout_swap)) }
            TrazoLink(onClick = { onExercise(ex.exerciseId) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.session_view_exercise)) }
        }
    }
}
