package com.trazo.android.data.repo

import android.content.Context
import com.trazo.android.data.api.AppVersionApi
import com.trazo.android.data.model.AppVersionPolicy
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.apiCall

/** What this build has to tell the user about updating. */
sealed interface UpdateNotice {
    data object None : UpdateNotice

    /** A newer version is suggested; [versionCode] is remembered when the user says "not now". */
    data class Recommended(val versionCode: Int) : UpdateNotice

    /** This version is no longer supported. */
    data object Required : UpdateNotice
}

interface AppVersionStore {
    fun policy(): AppVersionPolicy
    fun save(policy: AppVersionPolicy)
    /** The recommended version the user last dismissed, 0 if none. */
    fun dismissed(): Int
    fun dismiss(versionCode: Int)
}

class SharedPreferencesAppVersionStore(context: Context) : AppVersionStore {
    private val prefs = context.applicationContext.getSharedPreferences("app_version", Context.MODE_PRIVATE)
    override fun policy() = AppVersionPolicy(prefs.getInt(MINIMUM, 0), prefs.getInt(RECOMMENDED, 0))
    override fun save(policy: AppVersionPolicy) =
        prefs.edit().putInt(MINIMUM, policy.minimumAndroidVersionCode).putInt(RECOMMENDED, policy.recommendedAndroidVersionCode).apply()
    override fun dismissed() = prefs.getInt(DISMISSED, 0)
    override fun dismiss(versionCode: Int) = prefs.edit().putInt(DISMISSED, versionCode).apply()

    private companion object {
        const val MINIMUM = "minimum"
        const val RECOMMENDED = "recommended"
        const val DISMISSED = "dismissed"
    }
}

/**
 * The version policy of the server, kept on the phone so it also applies without a connection. It is asked when the app
 * opens and, after an answer, not again while the process lives. A failed request changes nothing.
 */
class AppVersionRepository(private val api: AppVersionApi, private val store: AppVersionStore, private val versionCode: Int) {
    private var answered = false

    fun notice(): UpdateNotice {
        val policy = store.policy()
        return when {
            versionCode < policy.minimumAndroidVersionCode -> UpdateNotice.Required
            versionCode < policy.recommendedAndroidVersionCode && store.dismissed() < policy.recommendedAndroidVersionCode ->
                UpdateNotice.Recommended(policy.recommendedAndroidVersionCode)
            else -> UpdateNotice.None
        }
    }

    suspend fun refresh(): UpdateNotice {
        if (!answered) {
            val result = apiCall { api.versionPolicy() }
            if (result is ApiResult.Success) {
                store.save(result.value)
                answered = true
            }
        }
        return notice()
    }

    fun dismiss(versionCode: Int) = store.dismiss(versionCode)
}
