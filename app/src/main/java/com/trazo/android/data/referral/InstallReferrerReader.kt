package com.trazo.android.data.referral

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Reads the Google Play Install Referrer once, on the first launch. When the invitation link sent the person to Google Play, the
 * store keeps `trazo_ref=CODE` and hands it to the app after installation: that is how an invitation survives "open link → install
 * → register later". It only works for installs made through Google Play and for a limited time after the click; anything else
 * (another store, a sideloaded APK, no link at all) simply has no referrer, and the person can type the code at registration.
 */
object InstallReferrerReader {
    /** The raw referrer string, or null when there is none or Google Play cannot answer. Never throws. */
    suspend fun read(context: Context): String? = suspendCancellableCoroutine { continuation ->
        val client = try {
            InstallReferrerClient.newBuilder(context.applicationContext).build()
        } catch (_: Exception) {
            continuation.resume(null); return@suspendCancellableCoroutine
        }
        fun finish(value: String?) {
            try { client.endConnection() } catch (_: Exception) { }
            if (continuation.isActive) continuation.resume(value)
        }
        try {
            client.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    finish(
                        if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK)
                            try { client.installReferrer.installReferrer } catch (_: Exception) { null }
                        else null
                    )
                }
                override fun onInstallReferrerServiceDisconnected() = finish(null)
            })
        } catch (_: Exception) {
            finish(null)
        }
        continuation.invokeOnCancellation { try { client.endConnection() } catch (_: Exception) { } }
    }
}
