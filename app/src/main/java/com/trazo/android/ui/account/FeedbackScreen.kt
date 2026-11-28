package com.trazo.android.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.android.R
import com.trazo.android.ui.common.ChipGroup
import com.trazo.android.ui.common.InlineError
import com.trazo.android.ui.common.MinTouch
import com.trazo.android.ui.common.Notice
import com.trazo.android.ui.common.TrazoTopBar
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoField
import com.trazo.android.ui.theme.TrazoPalette

private val CATEGORIES = listOf("bug", "idea", "other")

@Composable
fun FeedbackScreen(viewModel: FeedbackViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val labels = mapOf(
        "bug" to stringResource(R.string.feedback_bug),
        "idea" to stringResource(R.string.feedback_idea),
        "other" to stringResource(R.string.feedback_other)
    )
    Column(Modifier.fillMaxSize().imePadding()) {
        TrazoTopBar(stringResource(R.string.feedback_title), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.sent) {
                Notice(stringResource(R.string.feedback_sent))
            } else {
                Text(stringResource(R.string.feedback_note), color = TrazoPalette.TextSecondary)
                ChipGroup(CATEGORIES, setOf(state.category), { labels.getValue(it) }, viewModel::category)
                TrazoField(
                    state.message, viewModel::message, Modifier.fillMaxWidth(), { Text(stringResource(R.string.feedback_message)) },
                    singleLine = false, minLines = 5
                )
                state.error?.let { InlineError(it) }
                TrazoButton(onClick = viewModel::send, enabled = state.canSend, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) {
                    Text(stringResource(R.string.feedback_send))
                }
            }
        }
    }
}
