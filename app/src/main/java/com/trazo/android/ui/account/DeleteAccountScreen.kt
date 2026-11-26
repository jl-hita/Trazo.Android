package com.trazo.android.ui.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trazo.android.BuildConfig
import com.trazo.android.R
import com.trazo.android.auth.AuthRepository
import com.trazo.android.auth.GoogleResult
import com.trazo.android.auth.requestGoogleIdToken
import com.trazo.android.diagnostics.Diagnostics
import com.trazo.android.ui.common.*
import com.trazo.android.ui.googleResultText
import com.trazo.android.ui.theme.TrazoButton
import com.trazo.android.ui.theme.TrazoField
import com.trazo.android.ui.theme.TrazoPalette
import kotlinx.coroutines.launch

/** Explains what deleting the account does, asks the person to prove who they are and deletes it. */
@Composable
fun DeleteAccountScreen(viewModel: DeleteAccountViewModel, auth: AuthRepository, onBack: () -> Unit, onDeleted: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val open = rememberUrlOpener()
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var googleBusy by remember { mutableStateOf(false) }
    var googleMessage by remember { mutableStateOf<String?>(null) }
    val done = state.done

    // While the request is on its way, and once the account is gone, there is nothing to go back to.
    BackHandler(enabled = done != null || state.deleting) { if (done != null) onDeleted() }

    Column(Modifier.fillMaxSize()) {
        TrazoTopBar(stringResource(R.string.account_delete), onBack = if (done == null && !state.deleting) onBack else null)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val info = state.info
            when {
                done != null -> {
                    Text(stringResource(R.string.account_deleted))
                    if (done.playSubscriptionNotCancelled) {
                        info?.deletion?.warning?.let { Notice(it) }
                        done.manageSubscriptionUrl?.let { ManageSubscription { open(it) } }
                    }
                    TrazoButton(onClick = onDeleted, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) { Text(stringResource(R.string.accept)) }
                }
                state.loading -> Text(stringResource(R.string.loading), color = TrazoPalette.TextSecondary)
                info == null -> {
                    state.loadError?.let { InlineError(it) }
                    TrazoButton(onClick = viewModel::load, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) { Text(stringResource(R.string.retry)) }
                }
                !info.canDelete -> info.cannotDeleteReason?.let { Notice(it) }
                else -> {
                    val needs = info.deletion
                    Text(stringResource(R.string.account_delete_text))
                    needs.warning?.let { Notice(it) }
                    needs.manageSubscriptionUrl?.let { ManageSubscription { open(it) } }
                    if (needs.requiresPassword) TrazoField(password, { password = it }, Modifier.fillMaxWidth(), { Text(stringResource(R.string.login_password)) },
                        visualTransformation = PasswordVisualTransformation())
                    if (needs.requiresTwoFactorCode) TrazoField(code, { code = it }, Modifier.fillMaxWidth(), { Text(stringResource(R.string.login_2fa_code)) })
                    if (state.rejected) Text(stringResource(R.string.account_delete_rejected), color = TrazoPalette.Error)
                    state.deleteError?.let { InlineError(it) }
                    googleMessage?.let { Text(it, color = TrazoPalette.Error) }
                    val codeReady = !needs.requiresTwoFactorCode || code.isNotBlank()
                    if (needs.requiresGoogleIdToken) {
                        // Picking the Google account is the confirmation: the account is deleted with that token.
                        TrazoButton(
                            enabled = !state.deleting && !googleBusy && codeReady,
                            modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch),
                            onClick = {
                                googleBusy = true
                                googleMessage = null
                                scope.launch {
                                    try {
                                        when (val result = requestGoogleIdToken(context, BuildConfig.GOOGLE_WEB_CLIENT_ID, BuildConfig.DEBUG, auth.googleNonce())) {
                                            is GoogleResult.Token -> viewModel.delete(idToken = result.idToken, code = code)
                                            else -> googleMessage = googleResultText(context, result)
                                        }
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        Diagnostics.report(e)
                                        googleMessage = context.getString(R.string.google_unavailable)
                                    } finally {
                                        googleBusy = false
                                    }
                                }
                            }
                        ) { Text(stringResource(R.string.account_delete_google)) }
                    } else {
                        TrazoButton(
                            onClick = { viewModel.delete(password = password, code = code) },
                            enabled = !state.deleting && codeReady && (!needs.requiresPassword || password.isNotEmpty()),
                            modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)
                        ) { Text(stringResource(R.string.account_delete_confirm)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManageSubscription(onClick: () -> Unit) =
    TrazoButton(onClick = onClick, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) { Text(stringResource(R.string.account_delete_play)) }
