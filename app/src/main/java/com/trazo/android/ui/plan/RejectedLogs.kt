package com.trazo.android.ui.plan

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trazo.android.R
import com.trazo.android.data.local.PendingLog
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoLink
import java.time.Instant
import java.time.ZoneId

/** The workouts the server refused: each with its day, the reason, and the choice of sending it again or deleting it. */
@Composable
fun RejectedLogs(logs: List<PendingLog>, onRetry: (PendingLog) -> Unit, onDiscard: (PendingLog) -> Unit) {
    var discarding by remember { mutableStateOf<PendingLog?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        logs.forEach { log ->
            TrazoCard {
                Text(stringResource(R.string.log_rejected, formatDate(workoutDay(log))), style = MaterialTheme.typography.titleMedium)
                log.failure?.let { InlineError(it) }
                Text(stringResource(R.string.log_rejected_hint), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrazoLink(onClick = { onRetry(log) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.retry)) }
                    TrazoLink(onClick = { discarding = log }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.log_discard)) }
                }
            }
        }
    }
    discarding?.let { log ->
        AlertDialog(
            onDismissRequest = { discarding = null },
            text = { Text(stringResource(R.string.log_discard_confirm)) },
            confirmButton = { TextButton(onClick = { discarding = null; onDiscard(log) }) { Text(stringResource(R.string.log_discard)) } },
            dismissButton = { TextButton(onClick = { discarding = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/** The day of the workout in the device zone. Logs queued by an older version have no date: they are dated when queued. */
private fun workoutDay(log: PendingLog): String {
    val instant = log.request.performedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: Instant.ofEpochMilli(log.createdAtMillis)
    return instant.atZone(ZoneId.systemDefault()).toLocalDate().toString()
}
