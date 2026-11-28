package com.trazo.android.rest

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.trazo.android.diagnostics.Diagnostics

/** The offer to allow exact alarms, shown while a rest is running until the user allows them or turns it down. */
class ExactAlarmOffer(val isDue: () -> Boolean, val allow: () -> Unit, val decline: () -> Unit)

/** Remembers that the user said no, so the offer is not made again. */
class ExactAlarmChoice(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("rest", Context.MODE_PRIVATE)

    var declined: Boolean
        get() = prefs.getBoolean("exactDeclined", false)
        set(value) { prefs.edit().putBoolean("exactDeclined", value).apply() }
}

/** Opens the "Alarms & reminders" switch of the app; on a phone without that screen, the app's own settings. */
fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < 31) return
    val app = Uri.fromParts("package", context.packageName, null)
    runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, app)) }
        .recoverCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, app)) }
        .onFailure { Diagnostics.report(it) }
}
