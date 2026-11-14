package com.trazo.android.ui.common

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trazo.android.BuildConfig
import com.trazo.android.R
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.ui.theme.Eyebrow
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoPalette

/** Minimum touch target recommended by Android accessibility guidelines. */
val MinTouch = 48.dp

/**
 * Blocks screenshots, screen recording and the recents thumbnail while the routine is on screen (anti-copy). Debug builds
 * can switch it off with `-PsecureScreens=false`.
 */
@Composable
fun SecureScreen() {
    if (!BuildConfig.SECURE_SCREENS) return
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(Unit) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

@Composable
fun apiErrorText(error: ApiError): String = when {
    error.code == "no_active_plan" -> stringResource(R.string.err_no_active_plan)
    error.code == "active_plan_exists" -> stringResource(R.string.err_active_plan_exists)
    error.code == "plan_generation_failed" -> stringResource(R.string.err_plan_generation_failed)
    error.code == "session_out_of_window" -> stringResource(R.string.err_session_out_of_window)
    error.code == "session_already_logged" -> stringResource(R.string.err_session_already_logged)
    error.code == "purchase_already_linked" -> stringResource(R.string.err_purchase_already_linked)
    error.code == "purchase_account_mismatch" -> stringResource(R.string.err_purchase_account_mismatch)
    else -> when (error.kind) {
        ErrorKind.Network -> stringResource(R.string.err_network)
        ErrorKind.Server -> stringResource(R.string.err_server)
        ErrorKind.Unauthorized -> stringResource(R.string.err_unauthorized)
        ErrorKind.SubscriptionRequired -> stringResource(R.string.err_subscription)
        ErrorKind.NotFound -> stringResource(R.string.err_not_found)
        ErrorKind.Conflict -> stringResource(R.string.err_conflict)
        // Plan validation messages are written in Spanish by the server; show the first one when there is one.
        ErrorKind.Validation -> error.fields.values.firstOrNull()?.firstOrNull() ?: error.serverMessage ?: stringResource(R.string.err_validation)
        ErrorKind.Unknown -> stringResource(R.string.err_unknown)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrazoTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.size(MinTouch)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TrazoPalette.Background, titleContentColor = TrazoPalette.Text,
            navigationIconContentColor = TrazoPalette.Text, actionIconContentColor = TrazoPalette.Text
        )
    )
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier, label: String = stringResource(R.string.loading)) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = TrazoPalette.Link, modifier = Modifier.semantics { contentDescription = label })
            Text(label, color = TrazoPalette.TextSecondary)
        }
    }
}

@Composable
fun ErrorBox(error: ApiError, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(apiErrorText(error), color = TrazoPalette.Error, textAlign = TextAlign.Center)
            if (onRetry != null) TrazoButton(onClick = onRetry, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
fun EmptyBox(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = TrazoPalette.TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
fun InlineError(error: ApiError, modifier: Modifier = Modifier) {
    Text(apiErrorText(error), color = TrazoPalette.Error, modifier = modifier, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun Notice(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = TrazoPalette.WarningBackground,
        border = BorderStroke(1.dp, TrazoPalette.WarningBorder)
    ) { Text(text, color = TrazoPalette.WarningText, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) }
}

@Composable
fun TrazoCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = TrazoPalette.Card, contentColor = TrazoPalette.Text)
    val border = BorderStroke(1.dp, TrazoPalette.Border)
    val shape = RoundedCornerShape(10.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) = Eyebrow(text.uppercase(), modifier.padding(top = 12.dp, bottom = 4.dp))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipGroup(
    options: List<String>, selected: Set<String>, label: (String) -> String, onToggle: (String) -> Unit, modifier: Modifier = Modifier
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option in selected, onClick = { onToggle(option) }, label = { Text(label(option)) },
                modifier = Modifier.heightIn(min = MinTouch),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TrazoPalette.PrimaryButton, selectedLabelColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    }
}
