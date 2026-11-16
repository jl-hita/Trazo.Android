package com.trazo.android.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.widget.Toast
import com.trazo.android.R

/**
 * Copies a secret (the two-factor key) so it does not linger: Android 13+ is told the clip is sensitive (its preview is
 * hidden) and a minute later the clip is removed, but only if it is still the one we put there.
 */
object SecretClipboard {
    const val CLEAR_AFTER_MILLIS = 60_000L
    private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE" // ClipDescription.EXTRA_IS_SENSITIVE, API 33
    private val handler = Handler(Looper.getMainLooper())

    fun copy(context: Context, label: String, secret: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText(label, secret).apply {
            description.extras = PersistableBundle().apply { putBoolean(EXTRA_IS_SENSITIVE, true) }
        }
        clipboard.setPrimaryClip(clip)
        // Android 13+ shows its own "copied" confirmation; earlier versions need ours.
        if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, R.string.sec_2fa_copied, Toast.LENGTH_SHORT).show()
        val timestamp = clipboard.primaryClipDescription?.timestamp
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ clearIfUnchanged(clipboard, timestamp) }, CLEAR_AFTER_MILLIS)
    }

    /** The description can be read while the app is in the background (the content cannot), so the check does not need focus. */
    private fun clearIfUnchanged(clipboard: ClipboardManager, timestamp: Long?) {
        try {
            if (timestamp == null || clipboard.primaryClipDescription?.timestamp != timestamp) return
            if (Build.VERSION.SDK_INT >= 28) clipboard.clearPrimaryClip() else clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        } catch (_: Exception) { /* the system refused (background restrictions): the clip simply stays */ }
    }
}
