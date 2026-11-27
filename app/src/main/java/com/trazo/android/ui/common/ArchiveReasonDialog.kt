package com.trazo.android.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.trazo.android.R
import com.trazo.android.data.repo.ArchiveQuestion
import com.trazo.android.data.repo.ArchiveReason

/** Why the plan is archived: one tap answers and archives; closing or cancelling leaves the plan as it was. */
@Composable
fun ArchiveReasonDialog(question: ArchiveQuestion, onAnswer: (ArchiveReason?) -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.archive_reason_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                question.reasons.forEach { reason -> Answer(stringResource(reason.label)) { onAnswer(reason) } }
                Answer(stringResource(R.string.archive_reason_skip)) { onAnswer(null) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun Answer(text: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().heightIn(min = MinTouch).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.CenterStart) {
        Text(text)
    }
}

private val ArchiveReason.label: Int
    get() = when (this) {
        ArchiveReason.PlanCompleted -> R.string.archive_reason_completed
        ArchiveReason.WantNewPlan -> R.string.archive_reason_new_plan
        ArchiveReason.NoTime -> R.string.archive_reason_no_time
        ArchiveReason.TooDemanding -> R.string.archive_reason_too_hard
        ArchiveReason.TooEasy -> R.string.archive_reason_too_easy
        ArchiveReason.Discomfort -> R.string.archive_reason_discomfort
        ArchiveReason.GoalsChanged -> R.string.archive_reason_goals
        ArchiveReason.NotConvinced -> R.string.archive_reason_not_convinced
        ArchiveReason.Other -> R.string.archive_reason_other
    }
