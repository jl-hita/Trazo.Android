package com.trazo.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.trazo.android.ui.nav.AppRoot
import com.trazo.android.ui.theme.TrazoTheme

class MainActivity : ComponentActivity() {
    private var actionUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionUri = intent?.data
        setContent {
            TrazoTheme {
                val uri = actionUri
                val path = uri?.path.orEmpty()
                AppRoot(
                    actionType = when { path.endsWith("verify-email") -> "verify-email"; path.endsWith("reset-password") -> "reset-password"; else -> null },
                    actionToken = uri?.getQueryParameter("token")
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        actionUri = intent.data
    }
}
