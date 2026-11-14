package com.trazo.android.ui.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.android.R
import com.trazo.android.data.model.*
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoLink
import com.trazo.android.ui.theme.TrazoPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private fun appLocale(): Locale = if (Labels.english()) Locale.getDefault() else Locale("es", "ES")

fun formatDate(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(appLocale()))
}.getOrDefault(iso)

/** An ISO instant ("2026-11-14T10:01:00Z") as a short date and time in the device time zone. */
fun formatDateTime(iso: String): String = runCatching {
    java.time.OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(appLocale()))
}.getOrDefault(iso.take(16).replace('T', ' '))

@Composable
private fun statusLabel(status: String) = when (status) {
    "Completed" -> stringResource(R.string.status_completed)
    "Partial" -> stringResource(R.string.status_partial)
    "NotCompleted", "Skipped" -> stringResource(R.string.status_skipped)
    else -> stringResource(R.string.status_planned)
}

@Composable
fun PlanScreen(
    viewModel: PlanViewModel,
    onOpenSession: (String) -> Unit,
    onOverview: () -> Unit,
    onHistory: () -> Unit,
    onCreatePlan: () -> Unit,
    onSendPending: () -> Unit
) {
    SecureScreen()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pending by viewModel.pendingLogs.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(R.string.plan_title))
        val week = state.week
        when {
            state.loading && week == null -> LoadingBox()
            state.needsOnboarding -> OnboardingPrompt(onCreatePlan)
            state.error != null && week == null -> ErrorBox(state.error!!, viewModel::load)
            week != null -> LazyColumn(
                Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { WeekHeader(state.plan, week) }
                if (state.fromCache) item { Notice(stringResource(R.string.offline_copy)) }
                if (pending > 0) item {
                    Notice(stringResource(R.string.plan_pending_logs, pending))
                    TrazoLink(onClick = onSendPending, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.plan_send_now)) }
                }
                if (week.timeline == "Finished") item {
                    Notice(stringResource(R.string.plan_finished))
                    TrazoButton(onClick = viewModel::startNextPlan, modifier = Modifier.padding(top = 8.dp).heightIn(min = MinTouch)) { Text(stringResource(R.string.plan_next_plan)) }
                }
                if (week.timeline == "NotStarted") item { Notice(stringResource(R.string.plan_not_started, formatDate(week.weekStart))) }
                if (week.timeline == "PremiumRequired") item { Notice(stringResource(R.string.plan_generated_premium_locked)) }
                if (week.sessions.isEmpty() && week.timeline != "Finished" && week.timeline != "PremiumRequired") item { EmptyBox(stringResource(R.string.plan_no_sessions), Modifier.height(120.dp)) }
                items(week.sessions, key = { it.id }) { SessionCard(it) { onOpenSession(it.id) } }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrazoButton(onClick = onOverview, secondary = true, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.plan_overview)) }
                        TrazoButton(onClick = onHistory, secondary = true, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.plan_history)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPrompt(onCreatePlan: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.onb_intro), color = TrazoPalette.TextSecondary)
        Spacer(Modifier.height(16.dp))
        TrazoButton(onClick = onCreatePlan, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.onb_title)) }
    }
}

@Composable
private fun WeekHeader(plan: PlanView?, week: WeekView) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (plan != null) Text(stringResource(R.string.plan_week_of, week.weekNumber, plan.totalWeeks), style = MaterialTheme.typography.titleLarge)
        Text("${formatDate(week.weekStart)} – ${formatDate(week.weekEnd)}", color = TrazoPalette.TextSecondary)
        week.mesocycle?.let {
            val phase = Labels.phase(it.phaseType) + if (it.isDeload) " · ${stringResource(R.string.plan_deload)}" else ""
            Text(stringResource(R.string.plan_block, it.sequence, phase, it.weekInBlock, it.weeks), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SessionCard(session: SessionView, onClick: () -> Unit) {
    TrazoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(session.title, style = MaterialTheme.typography.titleMedium)
                Text("${Labels.day(session.day)} · ${formatDate(session.date)}", color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            val done = session.log != null
            Text(
                statusLabel(session.log?.outcome ?: session.status),
                color = if (done) TrazoPalette.Ok else TrazoPalette.Muted, style = MaterialTheme.typography.labelLarge
            )
        }
        Text(
            stringResource(R.string.plan_session_minutes, session.estimatedDurationMinutes) + " · " +
                stringResource(R.string.plan_session_exercises, session.exercises.size),
            color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun PlanOverviewScreen(viewModel: PlanViewModel, onBack: () -> Unit) {
    SecureScreen()
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(R.string.overview_title), onBack)
        val plan = state.plan
        if (plan == null) { if (state.loading) LoadingBox() else EmptyBox(stringResource(R.string.err_no_active_plan)) } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(plan.macrocycle.name, style = MaterialTheme.typography.titleLarge)
                    if (plan.macrocycle.notes.isNotBlank()) Text(plan.macrocycle.notes, color = TrazoPalette.TextSecondary)
                    Text("${formatDate(plan.startDate)} – ${formatDate(plan.endDate)}", color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                item { SectionTitle(stringResource(R.string.overview_phases)) }
                items(plan.macrocycle.phases, key = { it.sequence }) { phase ->
                    TrazoCard {
                        Text(phase.name, style = MaterialTheme.typography.titleMedium)
                        Text("${Labels.phase(phase.type)} · ${formatDate(phase.startDate)} – ${formatDate(phase.endDate)}", color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(phase.objective)
                    }
                }
                item { Notice(stringResource(R.string.overview_future_note)) }
                item { SectionTitle(stringResource(R.string.overview_profile)) }
                item {
                    TrazoCard {
                        Text(stringResource(R.string.overview_goal, Labels.goal(plan.goal)))
                        Text(stringResource(R.string.overview_level, Labels.experience(plan.experience)))
                        Text(stringResource(R.string.overview_days, Labels.inWeekOrder(plan.trainingDays).joinToString(", ") { Labels.day(it) }))
                        Text(
                            stringResource(
                                R.string.overview_limitations,
                                plan.limitations.takeIf { it.isNotEmpty() }?.joinToString(", ") { Labels.limitation(it) } ?: stringResource(R.string.overview_none)
                            )
                        )
                    }
                }
                item { Text(plan.disclaimer, color = TrazoPalette.Muted, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    SecureScreen()
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(R.string.history_title), onBack)
        when {
            state.loading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!, viewModel::load)
            state.items.isEmpty() -> EmptyBox(stringResource(R.string.history_empty))
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.items, key = { it.sessionId }) {
                    TrazoCard {
                        Text(it.title, style = MaterialTheme.typography.titleMedium)
                        Text("${formatDate(it.date)} · ${statusLabel(it.outcome)} · ${stringResource(R.string.history_sets, it.completedSets, it.plannedSets)}", color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
