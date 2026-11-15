package com.trazo.android.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.trazo.android.MainActivity
import com.trazo.android.R

/** The kinds of notification the server sends. The text is translated here so it follows the app language. */
object PushKinds {
    const val EXTRA_KIND = "trazo.push.kind"
    const val MAX_TITLE = 65
    const val MAX_BODY = 240

    private val bodies = mapOf(
        "review-reply" to R.string.push_review_reply,
        "review-closed" to R.string.push_review_closed,
        "review-new" to R.string.push_review_new,
        "review-message" to R.string.push_review_message,
        "test" to R.string.push_test
    )

    /** The app's own text for [kind], or null when the server's text is the one to show (announcements, newer kinds). */
    fun bodyRes(kind: String?): Int? = kind?.let(bodies::get)

    /** Kinds that belong to a plan review: tapping them opens the account screen, where the conversation is. */
    fun opensAccount(kind: String?): Boolean = kind != null && kind.startsWith("review-")

    /** Text from the network is shown as plain text, bounded, without control characters. */
    fun clean(text: String?, max: Int): String =
        text.orEmpty().filterNot { it.isISOControl() && it != '\n' }.trim().take(max)
}

/** Builds the notification for a data message. The app does it itself so it also works while the app is closed. */
object PushNotifier {
    const val CHANNEL_ID = "general"

    fun show(context: Context, data: Map<String, String>) {
        val kind = data["kind"]?.takeIf { it.length <= 40 }
        // "check" and unknown silent kinds are never shown.
        if (kind == "check") return
        val bodyRes = PushKinds.bodyRes(kind)
        val body = bodyRes?.let(context::getString) ?: PushKinds.clean(data["body"], PushKinds.MAX_BODY)
        if (body.isBlank()) return
        val title = if (bodyRes != null) context.getString(R.string.app_name)
        else PushKinds.clean(data["title"], PushKinds.MAX_TITLE).ifBlank { context.getString(R.string.app_name) }

        if (!canPost(context)) return
        ensureChannel(context)
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(PushKinds.EXTRA_KIND, kind.orEmpty())
        val pending = PendingIntent.getActivity(
            context, (kind ?: "").hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_trazo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // The lock screen shows only that there is a notification, never the text.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        // One notification per kind: a new reply replaces the previous one instead of piling up.
        runCatching { NotificationManagerCompat.from(context).notify(kind ?: "announcement", 1, notification) }
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.push_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.push_channel_description) }
        )
    }
}
