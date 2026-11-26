package com.trazo.android.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.trazo.android.R
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.HealthConsentRepository
import com.trazo.android.diagnostics.guarded
import com.trazo.android.diagnostics.launchGuarded
import com.trazo.android.ui.common.*
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoPalette

/** Whether TRAZO may use health data, and the way to withdraw it. It is given where such data is first entered, not here. */
@Composable
fun HealthSection(consent: HealthConsentRepository, onWithdrawn: () -> Unit) {
    var granted by remember { mutableStateOf(consent.granted()) }
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ApiError?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { guarded(R.string.feature_account) { consent.refresh(); granted = consent.granted() } }

    SectionTitle(stringResource(R.string.health_title))
    Text(stringResource(if (granted) R.string.health_on else R.string.health_off), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
    error?.let { InlineError(it) }
    if (granted) TrazoButton(onClick = { confirm = true }, enabled = !busy, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) {
        Text(stringResource(R.string.health_withdraw))
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(stringResource(R.string.health_withdraw)) },
        text = { Text(stringResource(R.string.health_withdraw_text)) },
        confirmButton = {
            TextButton(onClick = {
                confirm = false
                busy = true
                error = null
                scope.launchGuarded(R.string.feature_account, onFailure = { busy = false }) {
                    when (val result = consent.withdraw()) {
                        is ApiResult.Success -> { granted = false; onWithdrawn() }
                        is ApiResult.Failure -> error = result.error
                    }
                    busy = false
                }
            }) { Text(stringResource(R.string.health_withdraw_confirm)) }
        },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } }
    )
}
