package com.trazo.android.ui.account

import androidx.activity.compose.BackHandler
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
import com.trazo.android.diagnostics.Diagnostics
import com.trazo.android.auth.AuthRepository
import com.trazo.android.auth.*
import com.trazo.android.ui.authErrorText
import com.trazo.android.ui.googleResultText
import androidx.compose.ui.platform.LocalContext
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
    language: com.trazo.android.data.repo.LanguageRepository,
    user: UserSummary?,
    hasPlan: Boolean,
    generatedPlan: Boolean = false,
    onLogout: () -> Unit,
    onPremium: () -> Unit,
    onPlanArchived: () -> Unit,
    /** The texts the server sent are in the old language: ask again. */
    onLanguageChanged: () -> Unit = {},
    onReferrals: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }
    var reviewDialog by remember { mutableStateOf(false) }
    var reviewNote by remember { mutableStateOf("") }
    var testSent by remember { mutableStateOf(false) }
    LaunchedEffect(state.archived) { if (state.archived) { viewModel.consumeArchived(); onPlanArchived() } }

    BackHandler(enabled = state.threadOpen) { viewModel.closeThread() }
    if (state.threadOpen) {
        ReviewThread(state, onBack = viewModel::closeThread, onRefresh = viewModel::refreshThread, onDraft = viewModel::setDraft, onSend = viewModel::sendMessage)
        return
    }

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
                        if (status.source == "promo") status.promoExpiresAt?.let { Text(stringResource(R.string.account_promo_until, formatDate(it.take(10))), color = TrazoPalette.Ok) }
                    }
                }
            }
            if (state.status?.canUpgrade == true) {
                TrazoButton(onClick = onPremium, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) { Text(stringResource(R.string.premium_get)) }
            }
            TrazoButton(onClick = onReferrals, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) { Text(stringResource(R.string.referral_open)) }
            if (state.pendingLogs > 0) {
                Notice(stringResource(R.string.plan_pending_logs, state.pendingLogs))
                TrazoLink(onClick = viewModel::sendPending, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.plan_send_now)) }
            }

            if (hasPlan) {
            SectionTitle(stringResource(R.string.account_plan))
            state.archiveError?.let { InlineError(it) }
            TrazoButton(onClick = { confirmArchive = true }, enabled = !state.archiving, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) {
                Text(stringResource(R.string.account_archive))
            }

            // Manual review is for Premium users with an automatically generated plan.
            if (generatedPlan && state.status?.hasAccess == true) {
            SectionTitle(stringResource(R.string.account_review))
            TrazoCard {
                val review = state.review
                Text(stringResource(R.string.account_review_intro), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                when (review?.status) {
                    "pending" -> Text(stringResource(R.string.account_review_pending), color = TrazoPalette.Ok)
                    "in_progress" -> Text(stringResource(R.string.account_review_progress), color = TrazoPalette.Ok)
                    "reviewed" -> Text(stringResource(R.string.account_review_done, formatDate((review.reviewedAt ?: review.createdAt).take(10))), color = TrazoPalette.Ok)
                }
                if ((review?.unread ?: 0) > 0) Text(stringResource(R.string.account_review_unread, review!!.unread), color = TrazoPalette.Ok)
            }
            state.reviewError?.let { InlineError(it) }
            val open = state.review != null && state.review?.status != "reviewed"
            if (state.review != null) {
                TrazoButton(onClick = viewModel::openThread, secondary = open.not(), modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) {
                    Text(stringResource(R.string.account_review_open))
                }
            }
            if (!open) {
                TrazoButton(onClick = { reviewDialog = true }, enabled = !state.requestingReview, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) {
                    Text(stringResource(R.string.account_review_request))
                }
            }
            }
            }

            LanguageSection(language, onLanguageChanged)

            SectionTitle(stringResource(R.string.account_security))
            if (user != null) SecuritySection(auth, user, onLoggedOutEverywhere = onLogout)

            if (Diagnostics.isTester(user?.email)) {
                SectionTitle(stringResource(R.string.account_test_title))
                TrazoCard {
                    Text(stringResource(R.string.account_test_intro), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    if (testSent) Text(stringResource(R.string.account_test_sent), color = TrazoPalette.Ok)
                }
                TrazoButton(onClick = {
                    Diagnostics.report(IllegalStateException("Test handled error triggered on purpose from the app"), force = true)
                    testSent = true
                }, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) { Text(stringResource(R.string.account_test_handled)) }
                TrazoButton(onClick = { throw IllegalStateException("Test crash triggered on purpose from the app") },
                    secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) { Text(stringResource(R.string.account_test_crash)) }
            }

            Spacer(Modifier.height(16.dp))
            TrazoButton(onClick = { confirmLogout = true }, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)) { Text(stringResource(R.string.account_logout)) }
            Text(stringResource(R.string.account_version, BuildConfig.VERSION_NAME), color = TrazoPalette.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (reviewDialog) AlertDialog(
        onDismissRequest = { reviewDialog = false },
        title = { Text(stringResource(R.string.account_review_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.account_review_text))
                TrazoField(reviewNote, { if (it.length <= 1000) reviewNote = it }, Modifier.fillMaxWidth(), { Text(stringResource(R.string.account_review_note)) },
                    singleLine = false, minLines = 4)
            }
        },
        confirmButton = { TextButton(onClick = { reviewDialog = false; viewModel.requestReview(reviewNote); reviewNote = "" }) { Text(stringResource(R.string.account_review_send)) } },
        dismissButton = { TextButton(onClick = { reviewDialog = false }) { Text(stringResource(R.string.cancel)) } }
    )
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

/** Two-factor setup, Google linking and "sign out everywhere". Same endpoints the first login screen used. */
@Composable
private fun SecuritySection(auth: AuthRepository, initialUser: UserSummary, onLoggedOutEverywhere: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf(initialUser) }
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var setup by remember { mutableStateOf<TwoFactorSetupResponse?>(null) }
    var recovery by remember { mutableStateOf<List<String>>(emptyList()) }
    var confirmAll by remember { mutableStateOf(false) }
    fun str(id: Int) = context.getString(id)
    fun fail(e: Exception, fallback: Int) { Diagnostics.report(e); message = (e as? AuthException)?.let { authErrorText(context, it.code) } ?: str(fallback) }

    TrazoCard {
        Text(stringResource(if (user.twoFactorEnabled) R.string.sec_2fa_on else R.string.sec_2fa_off))
        if (user.twoFactorEnabled) {
            TrazoField(code, { code = it }, Modifier.fillMaxWidth(), { Text(stringResource(R.string.sec_2fa_disable_code)) })
            TrazoButton(enabled = !busy && code.isNotBlank(), modifier = Modifier.heightIn(min = MinTouch), onClick = {
                busy = true
                scope.launch {
                    try { auth.disableTwoFactor(code); code = ""; auth.currentUser()?.let { user = it }; message = str(R.string.sec_2fa_disabled) }
                    catch (e: Exception) { fail(e, R.string.auth_err_2fa_disable) } finally { busy = false }
                }
            }) { Text(stringResource(R.string.sec_2fa_disable)) }
        } else {
            TrazoButton(enabled = !busy, modifier = Modifier.heightIn(min = MinTouch), onClick = {
                busy = true
                scope.launch {
                    try { setup = auth.setupTwoFactor(); message = str(R.string.sec_2fa_setup_hint) }
                    catch (e: Exception) { fail(e, R.string.auth_err_2fa_setup) } finally { busy = false }
                }
            }) { Text(stringResource(R.string.sec_2fa_setup)) }
            setup?.let { details ->
                TwoFactorKey(details, onMessage = { message = it }, onError = { fail(it, R.string.sec_2fa_open_failed) })
                TrazoField(code, { code = it }, Modifier.fillMaxWidth(), { Text(stringResource(R.string.sec_2fa_six_digits)) })
                TrazoButton(enabled = !busy && code.isNotBlank(), modifier = Modifier.heightIn(min = MinTouch), onClick = {
                    busy = true
                    scope.launch {
                        try {
                            recovery = auth.enableTwoFactor(code).recoveryCodes; setup = null; code = ""
                            auth.currentUser()?.let { user = it }
                            message = str(R.string.sec_2fa_enabled)
                        } catch (e: Exception) { fail(e, R.string.auth_err_2fa_code) } finally { busy = false }
                    }
                }) { Text(stringResource(R.string.sec_2fa_enable)) }
            }
        }
        if (recovery.isNotEmpty()) {
            Text(stringResource(R.string.sec_2fa_recovery), style = MaterialTheme.typography.titleSmall)
            recovery.forEach { Text(it) }
        }
        message?.let { Text(it, color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall) }
    }

    if (user.googleLinked && !user.hasPassword) {
        TrazoButton(
            enabled = !busy,
            secondary = true,
            modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch),
            onClick = {
                busy = true
                scope.launch {
                    try {
                        auth.setupPassword()
                        message = str(R.string.sec_password_setup_sent)
                    } catch (e: Exception) {
                        fail(e, R.string.auth_err_generic)
                        if ((e as? AuthException)?.http == 409) message = str(R.string.sec_password_setup_conflict)
                    } finally { busy = false }
                }
            }
        ) { Text(stringResource(R.string.sec_password_setup)) }
    }

    if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank() && !user.googleLinked) TrazoButton(
        enabled = !busy, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch),
        onClick = {
            busy = true
            scope.launch {
                try {
                    when (val g = requestGoogleIdToken(context, BuildConfig.GOOGLE_WEB_CLIENT_ID, BuildConfig.DEBUG, auth.googleNonce())) {
                        is GoogleResult.Token -> { auth.linkGoogle(g.idToken); auth.currentUser()?.let { user = it }; message = str(R.string.sec_google_linked) }
                        else -> googleResultText(context, g)?.let { message = it }
                    }
                } catch (e: Exception) { fail(e, R.string.auth_err_link_google) } finally { busy = false }
            }
        }
    ) { Text(stringResource(R.string.sec_google_link)) }

    TrazoButton(enabled = !busy, secondary = true, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch), onClick = { confirmAll = true }) {
        Text(stringResource(R.string.sec_logout_all))
    }
    if (confirmAll) AlertDialog(
        onDismissRequest = { confirmAll = false },
        title = { Text(stringResource(R.string.sec_logout_all_title)) },
        text = { Text(stringResource(R.string.sec_logout_all_text)) },
        confirmButton = {
            TextButton(onClick = {
                confirmAll = false
                scope.launch {
                    try { auth.logoutAll(); onLoggedOutEverywhere() } catch (e: Exception) { fail(e, R.string.auth_err_logout_all) }
                }
            }) { Text(stringResource(R.string.sec_logout_all)) }
        },
        dismissButton = { TextButton(onClick = { confirmAll = false }) { Text(stringResource(R.string.cancel)) } }
    )
}

/** The language of the app and of what TRAZO sends (e-mails, notifications): follow the device or pick one of the languages of the app. */
@Composable
private fun LanguageSection(language: com.trazo.android.data.repo.LanguageRepository, onChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var choice by remember { mutableStateOf(language.choice()) }
    var expanded by remember { mutableStateOf(false) }
    SectionTitle(stringResource(R.string.account_language))
    Text(stringResource(R.string.account_language_hint), color = TrazoPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = MinTouch)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(context.getString(languageName(choice)))
                Spacer(Modifier.weight(1f))
                Text("▾")
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.94f)
        ) {
            com.trazo.android.data.local.LanguageChoice.ALL.forEach { option ->
                DropdownMenuItem(
                    text = { Text(context.getString(languageName(option))) },
                    onClick = {
                        expanded = false
                        if (option != choice) {
                            choice = option
                            scope.launch {
                                language.choose(option) // the choice stands on this device even if the server cannot be told now
                                onChanged()
                                (context as? android.app.Activity)?.recreate()
                            }
                        }
                    },
                    trailingIcon = if (option == choice) ({ Text("✓") }) else null
                )
            }
        }
    }
}

/** The name of a language in that language, so anyone finds theirs whatever the app is showing. */
private fun languageName(choice: String): Int = when (choice) {
    "auto" -> R.string.language_auto
    else -> languageNames.getValue(choice)
}

private val languageNames = mapOf(
    "en" to R.string.language_en, "es" to R.string.language_es, "ca" to R.string.language_ca, "gl" to R.string.language_gl,
    "eu" to R.string.language_eu, "pt" to R.string.language_pt, "fr" to R.string.language_fr, "it" to R.string.language_it,
    "de" to R.string.language_de, "nl" to R.string.language_nl, "nl-be" to R.string.language_nl_be, "da" to R.string.language_da,
    "nb" to R.string.language_nb, "sv" to R.string.language_sv, "fi" to R.string.language_fi, "pl" to R.string.language_pl,
    "cs" to R.string.language_cs, "sk" to R.string.language_sk, "sl" to R.string.language_sl, "hr" to R.string.language_hr,
    "sr" to R.string.language_sr, "bs" to R.string.language_bs, "mk" to R.string.language_mk, "ru" to R.string.language_ru,
    "el" to R.string.language_el, "tr" to R.string.language_tr, "ar" to R.string.language_ar, "zh" to R.string.language_zh,
    "ja" to R.string.language_ja, "ko" to R.string.language_ko
)
