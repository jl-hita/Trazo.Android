package com.trazo.android.ui.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

internal fun appLocale(): Locale = if (Labels.english()) Locale.getDefault() else Locale("es", "ES")

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
    onAdjustments: () -> Unit = {},
    onBlock: () -> Unit,
    onEditPlan: () -> Unit,
    onCreatePlan: () -> Unit,
    onCreateManualPlan: () -> Unit,
    onPremium: () -> Unit,
    onSendPending: () -> Unit,
    status: com.trazo.android.data.model.SubscriptionStatus? = null
) {
    SecureScreen()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pending by viewModel.pendingLogs.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        val week = state.week
        when {
            state.loading && week == null -> LoadingBox()
            state.needsOnboarding -> OnboardingPrompt(status, onCreatePlan, onCreateManualPlan, onPremium)
            state.error != null && week == null -> ErrorBox(state.error!!, viewModel::load)
            week != null -> LazyColumn(
                Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { WeekHeader(state.plan, week) }
                state.next?.let { next -> item { NextSessionCard(next, openable = { id -> week.sessions.any { it.id == id } || id == next.session?.id }, onOpenSession = onOpenSession, rescheduling = state.rescheduling, onReschedule = viewModel::reschedule, skipping = state.skipping, onSkip = viewModel::skip, onRebuild = viewModel::startNextPlan) } }
                if (state.fromCache) item { Notice(stringResource(R.string.offline_copy)) }
                if (pending > 0) item {
                    Notice(stringResource(R.string.plan_pending_logs, pending))
                    TrazoLink(onClick = onSendPending, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.plan_send_now)) }
                }
                if (week.timeline == "Finished") item {
                    Notice(stringResource(R.string.plan_finished))
                    TrazoButton(onClick = viewModel::startNextPlan, modifier = Modifier.padding(top = 8.dp).heightIn(min = MinTouch)) { Text(stringResource(R.string.plan_next_plan)) }
                }
                if (week.timeline == "NotStarted" && week.weekStart != null) item { Notice(stringResource(R.string.plan_not_started, formatDate(week.weekStart))) }
                if (week.locked) item {
                    Notice(stringResource(R.string.plan_generated_premium_locked))
                    TrazoButton(onClick = onPremium, modifier = Modifier.padding(top = 8.dp).heightIn(min = MinTouch)) { Text(stringResource(R.string.premium_get)) }
                }
                if (week.sessions.isEmpty() && week.timeline != "Finished" && !week.locked) item { EmptyBox(stringResource(R.string.plan_no_sessions), Modifier.height(120.dp)) }
                items(week.sessions, key = { it.id }) { SessionCard(it) { onOpenSession(it.id) } }
                // Editing rewrites the routines: by weekday in a calendar plan, in rotation order in a sequence plan.
                if (state.plan?.origin == "Manual") item {
                    TrazoButton(onClick = onEditPlan, secondary = true, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.plan_edit_button)) }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TrazoButton(
                                onClick = onBlock,
                                secondary = true,
                                modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                            ) {
                                Text(
                                    stringResource(R.string.plan_block_button),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                            TrazoButton(
                                onClick = onOverview,
                                secondary = true,
                                modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                            ) {
                                Text(
                                    stringResource(R.string.plan_overview),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                        TrazoButton(
                            onClick = onAdjustments,
                            secondary = true,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        ) {
                            Text(
                                stringResource(R.string.plan_adjustments),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        TrazoButton(
                            onClick = onHistory,
                            secondary = true,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        ) {
                            Text(
                                stringResource(R.string.plan_history),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPrompt(
    status: com.trazo.android.data.model.SubscriptionStatus?,
    onCreatePlan: () -> Unit,
    onCreateManualPlan: () -> Unit,
    onPremium: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.plan_empty_intro), style = MaterialTheme.typography.headlineSmall, color = TrazoPalette.Text, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        // The automatic plan is the Premium path, so it carries the lighter (primary) button.
        TrazoButton(onClick = onCreatePlan, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) { Text(stringResource(R.string.plan_create_auto)) }
        TrazoButton(onClick = onCreateManualPlan, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) { Text(stringResource(R.string.plan_create_manual)) }
        when {
            status?.canUpgrade == true -> TrazoCard {
                Text(stringResource(R.string.plan_auto_needs_premium), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 14.dp))
                TrazoButton(onClick = onPremium, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) { Text(stringResource(R.string.premium_get)) }
            }
            status != null && status.enforced && status.hasAccess -> TrazoCard {
                Text(stringResource(R.string.plan_premium_includes_title), style = MaterialTheme.typography.titleSmall, color = TrazoPalette.Text, modifier = Modifier.padding(bottom = 6.dp))
                Text(stringResource(R.string.plan_premium_includes), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun WeekHeader(plan: PlanView?, week: WeekView) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val sequence = plan?.schedulingMode == "Sequence"
        if (plan != null) Text(stringResource(if (sequence) R.string.plan_cycle_of else R.string.plan_week_of, week.weekNumber, plan.totalWeeks), style = MaterialTheme.typography.titleLarge)
        // A sequence plan has no calendar: only its position in the program is shown.
        if (week.weekStart != null && week.weekEnd != null) Text("${formatDate(week.weekStart)} – ${formatDate(week.weekEnd)}", color = TrazoPalette.TextSecondary)
        week.mesocycle?.let {
            val phase = Labels.phase(it.phaseType) + if (it.isDeload) " · ${stringResource(R.string.plan_deload)}" else ""
            Text(stringResource(if (sequence) R.string.plan_block_cycle else R.string.plan_block, it.sequence, phase, it.weekInBlock, it.weeks), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SessionCard(session: SessionView, onClick: () -> Unit) {
    TrazoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(session.title, style = MaterialTheme.typography.titleMedium)
                Text(sessionWhen(session), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
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
fun PlanOverviewScreen(viewModel: PlanViewModel, loadViewModel: LoadViewModel, onBack: () -> Unit) {
    SecureScreen()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val load by loadViewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(R.string.overview_title), onBack)
        val plan = state.plan
        if (plan == null) { if (state.loading) LoadingBox() else EmptyBox(stringResource(R.string.err_no_active_plan)) } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(plan.macrocycle.name, style = MaterialTheme.typography.titleLarge)
                    // The notes of a generated plan come from the engine and are not written for this app: only the user's own notes are shown.
                    if (plan.origin != "Generated") plan.macrocycle.notes?.takeIf { it.isNotBlank() }?.let { Text(it, color = TrazoPalette.TextSecondary) }
                    Text(
                        if (plan.endDate != null) "${formatDate(plan.startDate)} – ${formatDate(plan.endDate)}"
                        else stringResource(R.string.overview_cycles, plan.totalWeeks),
                        color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall
                    )
                }
                item {
                    SectionTitle(stringResource(if (load.load?.periodKind == "Rotation") R.string.overview_load_title_rotation else R.string.overview_load_title))
                    TrazoCard {
                        val data = load.load
                        when {
                            load.loading -> LoadingBox(Modifier.height(120.dp))
                            data == null -> Text(stringResource(R.string.overview_load_unavailable), color = TrazoPalette.TextSecondary)
                            else -> {
                                LoadChart(data, Modifier.fillMaxWidth())
                                Text(stringResource(if (data.periodKind == "Rotation") R.string.overview_load_note_rotation else R.string.overview_load_note), color = TrazoPalette.Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item { SectionTitle(stringResource(R.string.overview_phases)) }
                items(plan.macrocycle.phases, key = { it.sequence }) { phase ->
                    TrazoCard {
                        Text(phase.name, style = MaterialTheme.typography.titleMedium)
                        val range = if (phase.startDate != null && phase.endDate != null) "${formatDate(phase.startDate)} – ${formatDate(phase.endDate)}"
                        else stringResource(R.string.overview_phase_cycles, phase.startPeriod, phase.endPeriod)
                        Text("${Labels.phase(phase.type)} · $range", color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(phase.objective)
                    }
                }
                item { Notice(stringResource(R.string.overview_future_note)) }
                item { SectionTitle(stringResource(R.string.overview_profile)) }
                item {
                    TrazoCard {
                        Text(stringResource(R.string.overview_goal, Labels.goal(plan.goal)))
                        Text(stringResource(R.string.overview_level, Labels.experience(plan.experience)))
                        if (plan.schedulingMode == "Sequence") Text(stringResource(R.string.overview_sessions_per_cycle, plan.sessionsPerPeriod))
                        else Text(stringResource(R.string.overview_days, Labels.inWeekOrder(plan.trainingDays).joinToString(", ") { Labels.day(it) }))
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

/** Every adjustment the engine made (or decided not to make) with its numbers and the session that caused it. */
@Composable
fun AdaptationsScreen(viewModel: AdaptationsViewModel, onBack: () -> Unit) {
    SecureScreen()
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(R.string.adjustments_title), onBack)
        val history = state.history
        when {
            state.loading -> LoadingBox()
            state.error != null -> ErrorBox(state.error!!, viewModel::load)
            history == null || (history.entries.isEmpty() && !history.hasEarlier) ->
                EmptyBox(stringResource(if (history?.automatic == false) R.string.adjustments_empty_manual else R.string.adjustments_empty))
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        stringResource(if (state.wholePlan) R.string.adjustments_totals_plan else R.string.adjustments_totals_block, history.changes, history.sessionsAdjusted, history.reviews),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (history.entries.isEmpty()) item { Text(stringResource(R.string.adjustments_block_empty), color = TrazoPalette.TextSecondary) }
                // Earlier blocks are not forgotten, but the screen is about what is being trained now.
                if (history.hasEarlier || state.wholePlan) item {
                    TrazoLink(onClick = { viewModel.showWholePlan(!state.wholePlan) }, modifier = Modifier.heightIn(min = MinTouch)) {
                        Text(stringResource(if (state.wholePlan) R.string.adjustments_only_block else R.string.adjustments_whole_plan, history.planChanges))
                    }
                }
                items(history.entries, key = { it.id }) { entry ->
                    TrazoCard {
                        Text(entry.summary, style = MaterialTheme.typography.titleMedium, color = if (entry.changed) TrazoPalette.Text else TrazoPalette.TextSecondary)
                        if (entry.explanation.isNotBlank() && entry.explanation != entry.summary)
                            Text(entry.explanation, color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        val source = listOfNotNull(entry.sourceSession, entry.sourceDate?.let { formatDate(it) }).joinToString(" · ")
                        if (source.isNotBlank()) Text(stringResource(R.string.adjustments_after, source), color = TrazoPalette.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
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
                        // The day shown is the one the workout happened, not the one it was planned or registered.
                        val day = it.performedAt?.let { at -> formatDateTime(at) } ?: it.date?.let { d -> formatDate(d) } ?: ""
                        Text("$day · ${statusLabel(it.outcome)} · ${stringResource(R.string.history_sets, it.completedSets, it.plannedSets)}", color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
