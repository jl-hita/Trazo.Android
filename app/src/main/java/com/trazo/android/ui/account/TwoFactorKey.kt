package com.trazo.android.ui.account

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trazo.android.R
import com.trazo.android.auth.TwoFactorSetupResponse
import com.trazo.android.auth.isOtpAuthUri
import com.trazo.android.ui.common.MinTouch
import com.trazo.android.ui.common.SecretClipboard
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoPalette

/**
 * The key shown while two-factor is being set up: tap it to copy it, open the setup link straight in an authenticator app,
 * and an [i] button that explains how to use it.
 */
@Composable
fun TwoFactorKey(details: TwoFactorSetupResponse, onMessage: (String) -> Unit, onError: (Exception) -> Unit) {
    val context = LocalContext.current
    var showHelp by remember { mutableStateOf(false) }
    val label = stringResource(R.string.sec_2fa_secret_label)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.sec_2fa_secret), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        IconButton(onClick = { showHelp = true }, modifier = Modifier.size(MinTouch)) {
            Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.sec_2fa_info_cd))
        }
    }
    Text(
        details.secret,
        modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)
            .clickable(onClickLabel = stringResource(R.string.sec_2fa_copy)) { SecretClipboard.copy(context, label, details.secret) }
            .padding(vertical = 8.dp),
        style = MaterialTheme.typography.bodyLarge
    )
    Text(stringResource(R.string.sec_2fa_copy_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
    if (isOtpAuthUri(details.provisioningUri)) {
        TrazoButton(
            onClick = {
                try {
                    // A chooser, so the user sees which app receives the key.
                    val view = Intent(Intent.ACTION_VIEW, Uri.parse(details.provisioningUri))
                    context.startActivity(Intent.createChooser(view, context.getString(R.string.sec_2fa_open)))
                } catch (_: ActivityNotFoundException) {
                    onMessage(context.getString(R.string.sec_2fa_no_authenticator))
                } catch (e: Exception) {
                    onError(e)
                }
            },
            secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)
        ) { Text(stringResource(R.string.sec_2fa_open)) }
    }
    if (showHelp) AlertDialog(
        onDismissRequest = { showHelp = false },
        title = { Text(stringResource(R.string.sec_2fa_info_title)) },
        text = { Text(stringResource(R.string.sec_2fa_info_text)) },
        confirmButton = { TextButton(onClick = { showHelp = false }) { Text(stringResource(R.string.close)) } }
    )
}
