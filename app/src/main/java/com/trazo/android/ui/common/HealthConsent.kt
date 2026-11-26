package com.trazo.android.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trazo.android.R
import com.trazo.android.data.repo.HealthConsentRepository
import com.trazo.android.diagnostics.launchGuarded

/**
 * Runs the action that notes down health data, asking for the consent first when it has not been given. Declining simply does not run it.
 * Taking such data back (unmarking an injury, saying there was no longer pain) needs no consent and must not go through here.
 */
val LocalHealthGate = staticCompositionLocalOf<(() -> Unit) -> Unit> { { action -> action() } }

@Composable
fun HealthGate(consent: HealthConsentRepository, content: @Composable () -> Unit) {
    var waiting by remember { mutableStateOf<(() -> Unit)?>(null) }
    val scope = rememberCoroutineScope()
    val gate = remember(consent) { { action: () -> Unit -> if (consent.granted()) action() else waiting = action } }
    CompositionLocalProvider(LocalHealthGate provides gate, content = content)
    waiting?.let { action ->
        AlertDialog(
            onDismissRequest = { waiting = null },
            title = { Text(stringResource(R.string.health_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.health_text))
                    PrivacyLink()
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        waiting = null
                        consent.grant()
                        action()
                        scope.launchGuarded(R.string.feature_account) { consent.sync() }
                    },
                    modifier = Modifier.heightIn(min = MinTouch)
                ) { Text(stringResource(R.string.health_agree)) }
            },
            dismissButton = { TextButton(onClick = { waiting = null }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.health_decline)) } }
        )
    }
}
