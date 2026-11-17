package com.trazo.android.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    rescheduling: Boolean = false, onReschedule: () -> Unit = {}
) {
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
        if (next.overdue.isNotEmpty()) Notice(stringResource(R.string.next_overdue, next.overdue.size))
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
    }
}

/** The line under a session title: its day and date in a calendar plan, its place in the program in a sequence plan. */
@Composable
fun sessionWhen(session: SessionView): String {
    val date = session.date
    return if (date != null) "${session.day?.let { Labels.day(it) } ?: ""} · ${formatDate(date)}".trimStart(' ', '·')
    else stringResource(R.string.session_cycle_position, session.weekNumber, session.sequenceIndex)
}
