package com.trazo.android.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trazo.android.R
import com.trazo.android.data.model.NextSessionView
import com.trazo.android.data.model.SessionView
import com.trazo.android.ui.common.Labels
import com.trazo.android.ui.common.MinTouch
import com.trazo.android.ui.common.Notice
import com.trazo.android.ui.common.TrazoCard
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoLink
import com.trazo.android.ui.theme.TrazoPalette
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** "Ciclo 2 · sesión 1 de 3" / "Semana 2 · sesión 1 de 4": where the next session sits in the program. The backend decides it. */
@Composable
private fun positionLine(next: NextSessionView): String? {
    val position = next.position ?: return null
    return stringResource(
        if (next.schedulingMode == "Sequence") R.string.next_position_cycle else R.string.next_position_week,
        position.period, position.sessionInPeriod, position.sessionsInPeriod
    )
}

/** An ISO instant as a short date and time in the device zone ("16/11 20:30"): when the muscles are expected to be recovered. */
fun formatAvailableAt(iso: String): String = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(appLocale()))
}.getOrDefault(iso.take(16).replace('T', ' '))

/**
 * The session the user should do next. The program order and the recovery advice come from the backend; this only shows them.
 * Recovery is advice (Premium): it never prevents starting the workout.
 */
@Composable
fun NextSessionCard(
    next: NextSessionView, openable: (String) -> Boolean, onOpenSession: (String) -> Unit,
    rescheduling: Boolean = false, onReschedule: () -> Unit = {},
    skipping: Boolean = false, onSkip: (String, String) -> Unit = { _, _ -> }, onRebuild: () -> Unit = {}
) {
    var skipDialog by rememberSaveable { mutableStateOf(false) }
    var rebuildDialog by rememberSaveable { mutableStateOf(false) }
    val session: SessionView? = next.session
    TrazoCard {
        Text(stringResource(R.string.next_title), style = MaterialTheme.typography.labelLarge, color = TrazoPalette.TextSecondary)
        if (session == null) {
            Text(stringResource(R.string.next_none), color = TrazoPalette.TextSecondary)
            return@TrazoCard
        }
        Text(session.title, style = MaterialTheme.typography.titleLarge)
        positionLine(next)?.let { Text(it, color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall) }

        val availability = next.availability
        when {
            availability != null -> Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(
                        when (availability.state) {
                            "Ready" -> R.string.next_ready
                            "Caution" -> R.string.next_caution
                            else -> R.string.next_wait
                        }
                    ),
                    color = if (availability.state == "Ready") TrazoPalette.Ok else TrazoPalette.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
                availability.availableAt?.let { Text(stringResource(R.string.next_available_at, formatAvailableAt(it)), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall) }
                availability.reasons.forEach { Text("• $it", color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall) }
                availability.alternatives.filter { openable(it.sessionId) }.forEach { alt ->
                    TrazoLink(onClick = { onOpenSession(alt.sessionId) }, modifier = Modifier.heightIn(min = MinTouch)) {
                        Text(stringResource(R.string.next_alternative, alt.title))
                    }
                }
            }
            next.availabilityLocked -> Text(
                stringResource(R.string.next_recovery_locked), color = TrazoPalette.Muted,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)
            )
        }

        next.reentry?.let { Notice(it.message) }
        val adjustment = next.adjustment
        // The backend's advice replaces the plain count when it has something to suggest (omit the oldest late session).
        if (adjustment?.suggestSkip == true) Notice(adjustment.message)
        else if (next.overdue.isNotEmpty()) Notice(stringResource(R.string.next_overdue, next.overdue.size))
        // The plan may no longer fit the person (a very long absence or repeated omissions): suggest a new one, never rewrite it.
        if (adjustment?.strategy == "Rebuild") {
            Notice(adjustment.message)
            TrazoLink(onClick = { rebuildDialog = true }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.next_rebuild_action)) }
        }
        // The calendar went on without the program: offer to move what is pending to this week (the weekdays are kept).
        next.reschedule?.let { offer ->
            Notice(stringResource(R.string.next_reschedule, offer.weeksBehind))
            TrazoButton(onClick = onReschedule, secondary = true, enabled = !rescheduling, modifier = Modifier.padding(top = 8.dp).heightIn(min = MinTouch)) {
                Text(stringResource(if (rescheduling) R.string.next_reschedule_busy else R.string.next_reschedule_action))
            }
        }

        if (openable(session.id)) TrazoButton(onClick = { onOpenSession(session.id) }, modifier = Modifier.padding(top = 8.dp).heightIn(min = MinTouch)) {
            Text(stringResource(R.string.next_view))
        }
        // Omitting is always the user's decision: the session is not done, not counted as training and the plan goes on with the next one.
        if (adjustment?.canSkip != false) TrazoLink(onClick = { skipDialog = true }, modifier = Modifier.heightIn(min = MinTouch)) {
            Text(stringResource(if (skipping) R.string.next_skip_busy else R.string.next_skip_action))
        }
    }
    if (skipDialog) SkipDialog(
        enabled = !skipping, onDismiss = { skipDialog = false },
        onConfirm = { reason -> skipDialog = false; next.session?.id?.let { onSkip(it, reason) } }
    )
    if (rebuildDialog) AlertDialog(
        onDismissRequest = { rebuildDialog = false },
        title = { Text(stringResource(R.string.next_rebuild_title)) },
        text = { Text(stringResource(R.string.next_rebuild_text)) },
        confirmButton = { TextButton(onClick = { rebuildDialog = false; onRebuild() }) { Text(stringResource(R.string.next_rebuild_confirm)) } },
        dismissButton = { TextButton(onClick = { rebuildDialog = false }) { Text(stringResource(R.string.cancel)) } }
    )
}

/** The reasons an omitted session can carry. Agenda reasons change nothing else; Fatigue and Illness let the plan adapt (Premium). */
private val skipReasons = listOf("Time", "Equipment", "Motivation", "Fatigue", "Illness", "Other")

@Composable
private fun SkipDialog(enabled: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by rememberSaveable { mutableStateOf("Time") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.next_skip_title)) },
        text = {
            Column {
                Text(stringResource(R.string.next_skip_text), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                skipReasons.forEach { value ->
                    Row(Modifier.fillMaxWidth().heightIn(min = MinTouch).clickable { reason = value }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = reason == value, onClick = { reason = value })
                        Text(Labels.reason(value))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(reason) }, enabled = enabled) { Text(stringResource(R.string.next_skip_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/** The line under a session title: its day and date in a calendar plan, its place in the program in a sequence plan. */
@Composable
fun sessionWhen(session: SessionView): String {
    val date = session.date
    return if (date != null) "${session.day?.let { Labels.day(it) } ?: ""} · ${formatDate(date)}".trimStart(' ', '·')
    else stringResource(R.string.session_cycle_position, session.weekNumber, session.sequenceIndex)
}
