package com.trazo.android.push

import android.content.Context
import com.trazo.android.diagnostics.Diagnostics
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Where the Firebase options of the last sign-in are kept, so Firebase can start before the app has talked to the API. */
interface PushConfigStore {
    fun load(): PushClientConfig?
    fun save(config: PushClientConfig)
    fun clear()
    /** True once the notification permission has been requested, so it is asked only once. */
    var permissionAsked: Boolean
}

class SharedPreferencesPushConfigStore(context: Context) : PushConfigStore {
    private val prefs = context.applicationContext.getSharedPreferences("push", Context.MODE_PRIVATE)

    override fun load(): PushClientConfig? {
        val project = prefs.getString("projectId", null) ?: return null
        return PushClientConfig(
            true, project, prefs.getString("applicationId", null), prefs.getString("apiKey", null), prefs.getString("senderId", null)
        ).takeIf { it.usable }
    }

    override fun save(config: PushClientConfig) {
        prefs.edit().putString("projectId", config.projectId).putString("applicationId", config.applicationId)
            .putString("apiKey", config.apiKey).putString("senderId", config.senderId).apply()
    }

    override fun clear() { prefs.edit().remove("projectId").remove("applicationId").remove("apiKey").remove("senderId").apply() }

    override var permissionAsked: Boolean
        get() = prefs.getBoolean("permissionAsked", false)
        set(value) { prefs.edit().putBoolean("permissionAsked", value).apply() }
}

/**
 * Starts Firebase with the options the API serves (there is no google-services.json in the app) and keeps the device token
 * registered for the signed-in user. Everything here is best-effort: push is an extra, so no failure may reach the user.
 */
class PushRegistrar(
    private val context: Context,
    private val api: PushApi,
    private val store: PushConfigStore
) {
    /** Starts Firebase from the stored options (used when the process is woken by a message or a token refresh). */
    fun startFromCache(): Boolean = store.load()?.let(::ensureFirebase) ?: false

    /**
     * Fetches the options, starts Firebase and registers this phone for the signed-in user.
     * Returns true when push is enabled on the server (the caller then asks for the notification permission).
     */
    suspend fun register(): Boolean {
        val config = try {
            val response = api.config()
            if (!response.isSuccessful) return store.load() != null
            response.body() ?: return false
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline: keep what we had.
            return store.load() != null
        }
        if (!config.usable) { store.clear(); return false }
        store.save(config)
        if (!ensureFirebase(config)) return false
        val token = fetchToken() ?: return true
        try {
            api.register(RegisterDeviceBody(token))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Registered again at the next start.
        }
        return true
    }

    /** Tells the server this phone no longer belongs to the user and drops the token, so a signed-out phone gets nothing. */
    suspend fun unregister() {
        if (!startFromCache()) return
        val token = fetchToken()
        try {
            if (token != null) api.unregister(UnregisterDeviceBody(token))
            FirebaseMessaging.getInstance().deleteToken()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The server forgets the token when Firebase reports it as gone; what failed is still reported.
            Diagnostics.report(e)
        }
    }

    /** Drops this phone's token without calling the API, for an account that no longer exists. */
    suspend fun forget() {
        if (!startFromCache()) return
        try {
            FirebaseMessaging.getInstance().deleteToken()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Diagnostics.report(e)
        }
    }

    private fun ensureFirebase(config: PushClientConfig): Boolean = try {
        val options = FirebaseOptions.Builder()
            .setProjectId(config.projectId!!)
            .setApplicationId(config.applicationId!!)
            .setApiKey(config.apiKey!!)
            .apply { config.senderId?.takeIf { it.isNotBlank() }?.let(::setGcmSenderId) }
            .build()
        val existing = FirebaseApp.getApps(context).firstOrNull()
        when {
            existing == null -> { FirebaseApp.initializeApp(context, options); true }
            existing.options == options -> true
            else -> {
                // The superadmin pointed the app at another Firebase project: start over with the new options.
                existing.delete()
                FirebaseApp.initializeApp(context, options)
                true
            }
        }
    } catch (e: Exception) {
        Diagnostics.report(e)
        false
    }

    private suspend fun fetchToken(): String? = suspendCancellableCoroutine { cont ->
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) task.exception?.let { Diagnostics.report(it) }
                if (cont.isActive) cont.resume(if (task.isSuccessful) task.result?.takeIf { it.isNotBlank() } else null)
            }
        } catch (e: Exception) {
            Diagnostics.report(e)
            if (cont.isActive) cont.resume(null)
        }
    }
}
