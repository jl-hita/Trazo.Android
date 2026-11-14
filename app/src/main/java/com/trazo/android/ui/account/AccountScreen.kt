package com.trazo.android.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.android.BuildConfig
import com.trazo.android.R
import com.trazo.android.auth.AuthRepository
import com.trazo.android.auth.UserSummary
import com.trazo.android.ui.common.*
import com.trazo.android.ui.plan.formatDate
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoField
import com.trazo.android.ui.theme.TrazoLink
import com.trazo.android.ui.theme.TrazoPalette
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    auth: AuthRepository,
    user: UserSummary?,
    onLogout: () -> Unit,
    onPlanArchived: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }
    LaunchedEffect(state.archived) { if (state.archived) { viewModel.consumeArchived(); onPlanArchived() } }

    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(R.string.account_title))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            user?.let {
                TrazoCard {
                    Text(it.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(it.email, color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            SectionTitle(stringResource(R.string.account_subscription))
            TrazoCard {
                val status = state.status
                when {
                    status == null && state.statusError != null -> InlineError(state.statusError!!)
                    status == null -> Text(stringResource(R.string.loading), color = TrazoPalette.TextSecondary)
                    !status.enforced -> Text(stringResource(R.string.account_free))
                    else -> {
                        Text(stringResource(R.string.account_status, Labels.humanize(status.status)))
                        status.expiresAt?.let { Text(stringResource(R.string.account_expires, formatDate(it.take(10)))) }
                        if (status.hasAccess) Text(stringResource(R.string.account_access_ok), color = TrazoPalette.Ok)
                    }
                }
            }
            if (state.pendingLogs > 0) {
                Notice(stringResource(R.string.plan_pending_logs, state.pendingLogs))
                TrazoLink(onClick = viewModel::sendPending, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.plan_send_now)) }
            }

            SectionTitle(stringResource(R.string.account_plan))
            state.archiveError?.let { InlineError(it) }
            TrazoButton(onClick = { confirmArchive = true }, enabled = !state.archiving, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) {
                Text(stringResource(R.string.account_archive))
            }

            SectionTitle(stringResource(R.string.account_security))
            if (user != null) TwoFactorSection(auth, user)

            Spacer(Modifier.height(16.dp))
            TrazoButton(onClick = { confirmLogout = true }, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) { Text(stringResource(R.string.account_logout)) }
            Text(stringResource(R.string.account_version, BuildConfig.VERSION_NAME), color = TrazoPalette.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (confirmArchive) AlertDialog(
        onDismissRequest = { confirmArchive = false },
        title = { Text(stringResource(R.string.account_archive_title)) },
        text = { Text(stringResource(R.string.account_archive_text)) },
        confirmButton = { TextButton(onClick = { confirmArchive = false; viewModel.archivePlan() }) { Text(stringResource(R.string.account_archive_confirm)) } },
        dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text(stringResource(R.string.cancel)) } }
    )
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        title = { Text(stringResource(R.string.account_logout_title)) },
        text = {
            Text(
                stringResource(R.string.account_logout_text) +
                    if (state.pendingLogs > 0) "\n\n" + stringResource(R.string.account_logout_pending, state.pendingLogs) else ""
            )
        },
        confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text(stringResource(R.string.account_logout)) } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(stringResource(R.string.cancel)) } }
    )
}

/** Two-factor setup, as it was on the first login screen of the app: same endpoints, now under Account. */
@Composable
private fun TwoFactorSection(auth: AuthRepository, initialUser: UserSummary) {
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf(initialUser) }
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var setup by remember { mutableStateOf<com.trazo.android.auth.TwoFactorSetupResponse?>(null) }
    var recovery by remember { mutableStateOf<List<String>>(emptyList()) }

    TrazoCard {
        Text(if (user.twoFactorEnabled) "Autenticación en dos pasos: activada" else "Autenticación en dos pasos: desactivada")
        if (user.twoFactorEnabled) {
            TrazoField(code, { code = it }, Modifier.fillMaxWidth(), { Text("Código para desactivar 2FA") })
            TrazoButton(enabled = !busy && code.isNotBlank(), modifier = Modifier.heightIn(min = MinTouch), onClick = {
                busy = true
                scope.launch {
                    try { auth.disableTwoFactor(code); code = ""; auth.currentUser()?.let { user = it }; message = "Autenticación en dos pasos desactivada." }
                    catch (e: Exception) { message = e.message ?: "No se pudo desactivar 2FA." } finally { busy = false }
                }
            }) { Text("Desactivar 2FA") }
        } else {
            TrazoButton(enabled = !busy, modifier = Modifier.heightIn(min = MinTouch), onClick = {
                busy = true
                scope.launch {
                    try { setup = auth.setupTwoFactor(); message = "Añade la clave a tu aplicación autenticadora y confirma el código." }
                    catch (e: Exception) { message = e.message ?: "No se pudo iniciar 2FA." } finally { busy = false }
                }
            }) { Text("Configurar 2FA") }
            setup?.let { details ->
                Text("Clave secreta (guárdala de forma segura):", style = MaterialTheme.typography.titleSmall)
                Text(details.secret)
                TrazoField(code, { code = it }, Modifier.fillMaxWidth(), { Text("Código de 6 dígitos") })
                TrazoButton(enabled = !busy && code.isNotBlank(), modifier = Modifier.heightIn(min = MinTouch), onClick = {
                    busy = true
                    scope.launch {
                        try {
                            recovery = auth.enableTwoFactor(code).recoveryCodes; setup = null; code = ""
                            auth.currentUser()?.let { user = it }
                            message = "2FA activado. Guarda los códigos de recuperación: solo se muestran ahora."
                        } catch (e: Exception) { message = e.message ?: "El código no es válido." } finally { busy = false }
                    }
                }) { Text("Activar 2FA") }
            }
        }
        if (recovery.isNotEmpty()) {
            Text("Códigos de recuperación — guárdalos ahora", style = MaterialTheme.typography.titleSmall)
            recovery.forEach { Text(it) }
        }
        message?.let { Text(it, color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall) }
    }
}
