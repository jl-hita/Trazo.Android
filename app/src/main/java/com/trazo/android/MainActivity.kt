package com.trazo.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.trazo.android.push.PushKinds
import com.trazo.android.ui.nav.AppRoot
import com.trazo.android.ui.theme.TrazoTheme

class MainActivity : ComponentActivity() {
    private var actionUri by mutableStateOf<Uri?>(null)
    private var pushKind by mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.trazo.android.data.local.AppLocale.wrap(newBase, com.trazo.android.data.local.SharedPreferencesLanguageStore(newBase).choice()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionUri = trustedLink(intent)
        pushKind = pushKindOf(intent)
        setContent {
            TrazoTheme {
                val uri = actionUri
                val path = uri?.path.orEmpty()
                AppRoot(
                    actionType = when { path.endsWith("verify-email") -> "verify-email"; path.endsWith("reset-password") -> "reset-password"; else -> null },
                    actionToken = uri?.getQueryParameter("token"),
                    pushKind = pushKind,
                    onPushHandled = { pushKind = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        actionUri = trustedLink(intent)
        pushKind = pushKindOf(intent)
    }

    /** The kind of a tapped push notification (set by [com.trazo.android.push.PushNotifier]); empty when the app was opened another way. */
    private fun pushKindOf(intent: Intent?): String? =
        intent?.getStringExtra(PushKinds.EXTRA_KIND)?.takeIf { it.isNotBlank() && it.length <= 40 }

    /** Only https links to the site of this build carry an account action; anything else an app sends is ignored. */
    private fun trustedLink(intent: Intent?): Uri? =
        intent?.data?.takeIf { it.scheme == "https" && it.host.equals(BuildConfig.APP_LINK_HOST, ignoreCase = true) }
}
