package com.trazo.android.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.trazo.android.diagnostics.Diagnostics
import com.google.firebase.messaging.RemoteMessage
import com.trazo.android.TrazoApp

/** Receives Firebase's token refreshes and the data messages sent by the TRAZO API. */
class TrazoMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        // Only a signed-in user has a device to register; the next sign-in registers the new token anyway.
        try { (application as? TrazoApp)?.container?.registerPushIfSignedIn() } catch (e: Exception) { Diagnostics.report(e) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // A service that throws takes the whole process down: a notification that cannot be shown is reported and dropped.
        try { PushNotifier.show(applicationContext, message.data) } catch (e: Exception) { Diagnostics.report(e) }
    }
}
