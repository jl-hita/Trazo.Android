package com.trazo.android.ui.billing

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.heightIn
import com.trazo.android.R
import com.trazo.android.ui.common.MinTouch

/**
 * What a free account sees when it asks for an automatic plan: what it can do (manual plans, logging, history) and the two
 * ways forward, instead of a bare "you need a subscription".
 */
@Composable
fun PremiumRequiredDialog(onPremium: () -> Unit, onManualPlan: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.premium_auto_title)) },
        text = { Text(stringResource(R.string.premium_auto_text)) },
        confirmButton = {
            TextButton(onClick = onPremium, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.premium_get)) }
        },
        dismissButton = {
            TextButton(onClick = onManualPlan, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.premium_auto_manual)) }
        }
    )
}
