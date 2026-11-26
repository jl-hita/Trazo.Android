package com.trazo.android.rest

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.trazo.android.MainActivity
import com.trazo.android.R
import com.trazo.android.diagnostics.Diagnostics
import com.trazo.android.push.PushNotifier

/** Wakes the phone when a rest between sets is over, even if the app is in the background or the screen is off. */
interface RestAlarmScheduler {
    /** Plans the alert for [atMillis] (wall clock), replacing any previous one: there is a single rest at a time. */
    fun schedule(atMillis: Long)
    fun cancel()

    object None : RestAlarmScheduler {
        override fun schedule(atMillis: Long) {}
        override fun cancel() {}
    }
}

/**
 * Exact alarm that works in Doze. Since Android 12 the exact alarm permission can be revoked by the user: then the alert
 * still comes (an inexact alarm that is allowed while idle), a bit later, instead of not coming at all.
 */
class AndroidRestAlarmScheduler(context: Context) : RestAlarmScheduler {
    private val app = context.applicationContext
    private val alarms get() = app.getSystemService(AlarmManager::class.java)

    private fun pending() = PendingIntent.getBroadcast(
        app, REQUEST_CODE, Intent(app, RestAlarmReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    override fun schedule(atMillis: Long) {
        val manager = alarms ?: return
        val intent = pending()
        RestNotifier.dismiss(app)
        runCatching {
            if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
        }.onFailure {
            // The permission was revoked between the check and the call.
            runCatching { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent) }.onFailure { e -> Diagnostics.report(e) }
        }
    }

    override fun cancel() {
        runCatching { alarms?.cancel(pending()) }.onFailure { Diagnostics.report(it) }
        RestNotifier.dismiss(app)
    }

    companion object {
        const val ACTION = "com.trazo.android.REST_OVER"
        private const val REQUEST_CODE = 4101
    }
}

/** Tracks whether an activity of the app is visible: the rest alert is only for when the user is somewhere else. */
object AppForeground : Application.ActivityLifecycleCallbacks {
    @Volatile private var started = 0
    val isForeground: Boolean get() = started > 0

    fun install(app: Application) = app.registerActivityLifecycleCallbacks(this)

    override fun onActivityStarted(activity: Activity) { started++; RestNotifier.dismiss(activity) }
    override fun onActivityStopped(activity: Activity) { if (started > 0) started-- }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}

class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AndroidRestAlarmScheduler.ACTION) return
        // With the app in view the screen already shows the end of the rest.
        if (AppForeground.isForeground) return
        RestNotifier.show(context)
    }
}

/** The "rest is over" notification, in its own channel: short sound and vibration, high priority. */
object RestNotifier {
    const val CHANNEL_ID = "rest"
    private const val TAG = "rest"
    private const val ID = 2

    @SuppressLint("MissingPermission") // canPost() checks the permission before notify().
    fun show(context: Context) {
        if (!PushNotifier.canPost(context)) return
        ensureChannel(context)
        val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, ID, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_trazo)
            .setContentTitle(context.getString(R.string.rest_over_title))
            .setContentText(context.getString(R.string.rest_over_text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(10 * 60 * 1000L)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(TAG, ID, notification) }.onFailure { Diagnostics.report(it) }
    }

    fun dismiss(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(TAG, ID) }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.rest_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.rest_channel_description)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }
        )
    }
}
