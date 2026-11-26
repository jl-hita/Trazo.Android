package com.trazo.android.ui.billing

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.android.R
import com.trazo.android.ui.account.PaywallViewModel
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoLink
import com.trazo.android.ui.theme.TrazoPalette

/**
 * Premium offer. It is opened on demand (from the account, a locked plan or a Premium-only feature); nothing forces the user
 * to see it, since manual plans are free.
 */
@Composable
fun PaywallScreen(viewModel: PaywallViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.paywall_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.paywall_text), color = TrazoPalette.TextSecondary)
        Spacer(Modifier.height(24.dp))
        when {
            state.loading -> CircularProgressIndicator(color = TrazoPalette.Link)
            state.offer != null -> TrazoCard {
                val offer = state.offer!!
                Text(offer.title, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.paywall_price, offer.formattedPrice))
                if (offer.hasFreeTrial) Text(stringResource(R.string.paywall_trial), color = TrazoPalette.Ok, style = MaterialTheme.typography.bodySmall)
            }
            state.unavailable -> Notice(stringResource(R.string.paywall_unavailable))
        }
        Spacer(Modifier.height(16.dp))
        if (state.pending) Notice(stringResource(R.string.paywall_pending))
        if (state.failed) Notice(stringResource(R.string.paywall_failed))
        state.error?.let { InlineError(it) }
        Spacer(Modifier.height(16.dp))
        TrazoButton(
            onClick = { activity?.let(viewModel::buy) },
            enabled = state.offer != null && !state.purchasing && activity != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)
        ) { Text(stringResource(R.string.paywall_subscribe)) }
        TrazoLink(onClick = viewModel::restore, enabled = !state.purchasing, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.paywall_restore)) }
        TrazoLink(onClick = onClose, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.paywall_close)) }
    }
}
