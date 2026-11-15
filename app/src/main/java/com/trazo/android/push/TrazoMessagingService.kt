package com.trazo.android.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.trazo.android.TrazoApp

/** Receives Firebase's token refreshes and the data messages sent by the TRAZO API. */
class TrazoMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        // Only a signed-in user has a device to register; the next sign-in registers the new token anyway.
        (application as? TrazoApp)?.container?.registerPushIfSignedIn()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        PushNotifier.show(applicationContext, message.data)
    }
}
