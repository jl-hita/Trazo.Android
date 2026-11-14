package com.trazo.android.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.trazo.android.BuildConfig
import com.trazo.android.R
import com.trazo.android.auth.*
import com.trazo.android.ui.theme.*
import kotlinx.coroutines.launch

/** Text for an authentication failure, in the user's language. */
fun authErrorText(context: Context, code: AuthError): String = context.getString(
    when (code) {
        AuthError.Generic -> R.string.auth_err_generic
        AuthError.IncompleteResponse -> R.string.auth_err_incomplete
        AuthError.NoChallenge -> R.string.auth_err_no_challenge
        AuthError.NoSession -> R.string.auth_err_no_session
        AuthError.TwoFactorSetup -> R.string.auth_err_2fa_setup
        AuthError.TwoFactorCode -> R.string.auth_err_2fa_code
        AuthError.TwoFactorDisable -> R.string.auth_err_2fa_disable
        AuthError.LogoutAll -> R.string.auth_err_logout_all
        AuthError.LinkGoogle -> R.string.auth_err_link_google
    }
)

fun googleResultText(context: Context, result: GoogleResult): String? = when (result) {
    is GoogleResult.Token, GoogleResult.Cancelled -> null
    GoogleResult.NoAccount -> context.getString(R.string.google_no_account)
    GoogleResult.UnexpectedCredential -> context.getString(R.string.google_unexpected)
    is GoogleResult.Unavailable -> result.detail?.let { context.getString(R.string.google_failed_debug, it) } ?: context.getString(R.string.google_unavailable)
}

@Composable
fun LoginScreen(
    repo: AuthRepository,
    googleClientId: String,
    actionType: String? = null,
    actionToken: String? = null,
    onSignedIn: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var challenge by remember { mutableStateOf<String?>(null) }
    var newPassword by remember { mutableStateOf("") }
    var register by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    fun str(id: Int) = context.getString(id)

    LaunchedEffect(actionType, actionToken) {
        if (actionType == "verify-email" && !actionToken.isNullOrBlank()) {
            message = try {
                if (repo.verifyEmail(actionToken).isSuccessful) str(R.string.login_email_verified) else str(R.string.login_link_invalid)
            } catch (_: Exception) { str(R.string.login_verify_failed) }
        }
    }

    fun handle(result: Outcome) {
        when (result) {
            Outcome.Success -> { challenge = null; onSignedIn() }
            is Outcome.Challenge -> { challenge = result.token; message = str(R.string.login_enter_2fa) }
            is Outcome.EmailVerificationRequired -> message = result.serverMessage ?: str(R.string.login_verify_email_first)
            is Outcome.Error -> message = authErrorText(context, result.code)
        }
    }

    Surface(Modifier.fillMaxSize(), color = TrazoPalette.Background, contentColor = TrazoPalette.Text) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.widthIn(max = 440.dp).fillMaxWidth().border(1.dp, TrazoPalette.Border, RoundedCornerShape(10.dp))
                    .background(TrazoPalette.Card, RoundedCornerShape(10.dp)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(painterResource(R.drawable.trazo_wordmark_dark), contentDescription = stringResource(R.string.app_name), modifier = Modifier.height(64.dp))
                Eyebrow(stringResource(R.string.login_tagline))
                Spacer(Modifier.height(24.dp))
                if (actionType == "reset-password" && !actionToken.isNullOrBlank()) {
                    Text(stringResource(R.string.login_reset_title), style = MaterialTheme.typography.titleLarge)
                    TrazoField(newPassword, { newPassword = it }, Modifier.fillMaxWidth(), { Text(stringResource(R.string.login_new_password)) }, visualTransformation = PasswordVisualTransformation())
                    TrazoButton(enabled = !busy && newPassword.length >= 12, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                        busy = true
                        scope.launch {
                            message = try {
                                if (repo.resetPassword(actionToken, newPassword).isSuccessful) str(R.string.login_password_updated) else str(R.string.login_link_invalid)
                            } catch (_: Exception) { str(R.string.login_reset_failed) } finally { busy = false }
                        }
                    }) { Text(stringResource(R.string.login_save_password)) }
                } else if (challenge != null) {
                    TrazoField(code, { code = it }, Modifier.fillMaxWidth(), { Text(stringResource(R.string.login_2fa_code)) })
                    TrazoButton(enabled = !busy && code.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                        busy = true
                        scope.launch { try { handle(repo.verify(challenge!!, code)) } catch (_: Exception) { message = str(R.string.login_2fa_failed) } finally { busy = false } }
                    }) { Text(stringResource(R.string.login_verify)) }
                } else {
                    if (register) TrazoField(name, { name = it }, Modifier.fillMaxWidth(), { Text(stringResource(R.string.login_name)) })
                    TrazoField(email, { email = it }, Modifier.fillMaxWidth(), { Text(stringResource(R.string.login_email)) }, KeyboardOptions(keyboardType = KeyboardType.Email))
                    TrazoField(password, { password = it }, Modifier.fillMaxWidth(), { Text(stringResource(R.string.login_password)) }, visualTransformation = PasswordVisualTransformation())
                    Spacer(Modifier.height(12.dp))
                    TrazoButton(
                        enabled = !busy && email.isNotBlank() && password.isNotBlank() && (!register || name.isNotBlank()),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        onClick = {
                            busy = true
                            scope.launch {
                                try { handle(if (register) repo.register(email, password, name) else repo.login(email, password)) }
                                catch (_: Exception) { message = str(R.string.login_connect_failed) } finally { busy = false }
                            }
                        }
                    ) { Text(stringResource(if (register) R.string.login_create_account else R.string.login_sign_in)) }
                    TrazoLink(onClick = { register = !register; message = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(if (register) R.string.login_have_account else R.string.login_create_account))
                    }
                    TrazoLink(enabled = email.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp), onClick = {
                        scope.launch {
                            message = try { repo.resendVerification(email); str(R.string.login_resend_sent) } catch (_: Exception) { str(R.string.login_resend_failed) }
                        }
                    }) { Text(stringResource(R.string.login_resend)) }
                    TrazoLink(enabled = email.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp), onClick = {
                        scope.launch {
                            message = try { repo.forgotPassword(email); str(R.string.login_forgot_sent) } catch (_: Exception) { str(R.string.login_forgot_failed) }
                        }
                    }) { Text(stringResource(R.string.login_forgot)) }
                    if (googleClientId.isNotBlank()) {
                        TrazoButton(enabled = !busy, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    when (val g = requestGoogleIdToken(context, googleClientId, BuildConfig.DEBUG)) {
                                        is GoogleResult.Token -> handle(repo.google(g.idToken))
                                        else -> message = googleResultText(context, g)
                                    }
                                } catch (_: Exception) { message = str(R.string.login_connect_failed) } finally { busy = false }
                            }
                        }) { Text(stringResource(R.string.login_google)) }
                    } else Text(stringResource(R.string.login_google_missing), style = MaterialTheme.typography.bodySmall)
                }
                message?.let { Spacer(Modifier.height(12.dp)); Text(it) }
            }
        }
    }
}
