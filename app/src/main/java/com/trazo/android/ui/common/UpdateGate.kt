package com.trazo.android.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trazo.android.R
import com.trazo.android.data.repo.AppVersionRepository
import com.trazo.android.data.repo.UpdateNotice
import com.trazo.android.diagnostics.Diagnostics
import com.trazo.android.diagnostics.guarded

/**
 * Shows the app, or only the offer to update when this version is no longer supported. Nothing on the phone is touched: queued
 * workouts and drafts are still there after updating.
 */
@Composable
fun UpdateGate(versions: AppVersionRepository, content: @Composable () -> Unit) {
    var notice by remember { mutableStateOf(versions.notice()) }
    LaunchedEffect(Unit) { guarded(R.string.feature_session) { notice = versions.refresh() } }
    val context = LocalContext.current

    if (notice == UpdateNotice.Required) {
        UpdateRequiredScreen { openStore(context) }
        return
    }
    content()
    (notice as? UpdateNotice.Recommended)?.let { recommended ->
        val later = {
            versions.dismiss(recommended.versionCode)
            notice = UpdateNotice.None
        }
        AlertDialog(
            onDismissRequest = later,
            title = { Text(stringResource(R.string.update_available_title)) },
            text = { Text(stringResource(R.string.update_available_body)) },
            confirmButton = { TextButton(onClick = { later(); openStore(context) }) { Text(stringResource(R.string.update_action)) } },
            dismissButton = { TextButton(onClick = later) { Text(stringResource(R.string.update_later)) } }
        )
    }
}

@Composable
fun UpdateRequiredScreen(onUpdate: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.update_required_title), Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(stringResource(R.string.update_required_body), textAlign = TextAlign.Center)
        Button(onClick = onUpdate, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.update_action)) }
    }
}

/** The page of the app in Google Play; the web page when the phone has no Play app. */
private fun openStore(context: Context) {
    val id = context.packageName
    for (address in listOf("market://details?id=$id", "https://play.google.com/store/apps/details?id=$id")) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(address)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (e: ActivityNotFoundException) {
            if (address.startsWith("https")) Diagnostics.report(e)
        }
    }
}
