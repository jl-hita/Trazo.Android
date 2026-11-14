package com.trazo.android.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trazo.android.R
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoField
import com.trazo.android.ui.theme.TrazoPalette

/** The conversation with the team about the manual review of the plan. */
@Composable
internal fun ReviewThread(
    state: AccountState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onDraft: (String) -> Unit,
    onSend: () -> Unit
) {
    val closed = state.review?.status == "reviewed"
    val listState = rememberLazyListState()
    LaunchedEffect(state.thread.size) { if (state.thread.isNotEmpty()) listState.animateScrollToItem(state.thread.lastIndex) }

    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(
            stringResource(R.string.account_review),
            onBack = onBack,
            actions = { TextButton(onClick = onRefresh) { Text(stringResource(R.string.account_review_refresh)) } }
        )
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            if (state.thread.isEmpty()) {
                Text(stringResource(R.string.account_review_empty), color = TrazoPalette.TextSecondary, modifier = Modifier.padding(vertical = 16.dp))
            }
            LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.thread, key = { it.id }) { message ->
                    val mine = message.author == "user"
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                        TrazoCard(Modifier.fillMaxWidth(0.88f)) {
                            Text(
                                stringResource(if (mine) R.string.account_review_you else R.string.account_review_team) + " · " +
                                    message.createdAt.take(16).replace('T', ' '),
                                color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.labelSmall
                            )
                            Text(message.body)
                        }
                    }
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.reviewError?.let { InlineError(it) }
            if (closed) {
                Notice(stringResource(R.string.account_review_closed))
            } else {
                TrazoField(state.draft, onDraft, Modifier.fillMaxWidth(), { Text(stringResource(R.string.account_review_message)) }, singleLine = false, minLines = 3)
                TrazoButton(onClick = onSend, enabled = state.draft.isNotBlank() && !state.requestingReview, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) {
                    Text(stringResource(R.string.account_review_send))
                }
            }
        }
    }
}
