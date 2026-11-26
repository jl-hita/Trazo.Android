package com.trazo.android.ui.common

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.trazo.android.BuildConfig
import com.trazo.android.R
import com.trazo.android.diagnostics.Diagnostics
import com.trazo.android.ui.theme.TrazoLink
import java.util.Locale

/** The legal pages of the site that serves the API. They exist in Spanish and English: every other language reads the English ones. */
data class LegalUrls(val privacy: String, val terms: String)

fun legalUrls(host: String, language: String): LegalUrls =
    if (language == "es") LegalUrls("https://$host/privacidad", "https://$host/condiciones")
    else LegalUrls("https://$host/privacy-policy", "https://$host/terms")

/** Opens a web page; a phone without a browser only reports it. */
@Composable
fun rememberUrlOpener(): (String) -> Unit {
    val handler = LocalUriHandler.current
    return { url -> try { handler.openUri(url) } catch (e: IllegalArgumentException) { Diagnostics.report(e) } }
}

@Composable
fun PrivacyLink() {
    val open = rememberUrlOpener()
    val url = legalUrls(BuildConfig.APP_LINK_HOST, Locale.getDefault().language).privacy
    TrazoLink(onClick = { open(url) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.legal_privacy)) }
}

@Composable
fun LegalLinks() {
    val open = rememberUrlOpener()
    val urls = legalUrls(BuildConfig.APP_LINK_HOST, Locale.getDefault().language)
    PrivacyLink()
    TrazoLink(onClick = { open(urls.terms) }, modifier = Modifier.heightIn(min = MinTouch)) { Text(stringResource(R.string.legal_terms)) }
}
