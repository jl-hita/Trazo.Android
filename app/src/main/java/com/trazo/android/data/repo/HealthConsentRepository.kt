package com.trazo.android.data.repo

import android.content.Context
import com.trazo.android.data.api.AccountApi
import com.trazo.android.data.model.HealthConsentRequest
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.apiCall

/** What this phone knows about the consent of the signed-in user. */
interface HealthConsentStore {
    fun granted(): Boolean
    /** Given on this phone and not yet told to the server. */
    fun pending(): Boolean
    fun save(granted: Boolean, pending: Boolean)
}

class SharedPreferencesHealthConsentStore(context: Context) : HealthConsentStore {
    private val prefs = context.applicationContext.getSharedPreferences("health-consent", Context.MODE_PRIVATE)
    override fun granted() = prefs.getBoolean(GRANTED, false)
    override fun pending() = prefs.getBoolean(PENDING, false)
    override fun save(granted: Boolean, pending: Boolean) { prefs.edit().putBoolean(GRANTED, granted).putBoolean(PENDING, pending).apply() }
    private companion object { const val GRANTED = "granted"; const val PENDING = "pending" }
}

/**
 * The explicit, optional consent for health data (injuries, pain, discomfort, illness). The server keeps the record; this phone keeps a
 * copy so the question is not repeated and a workout can be noted down offline.
 */
class HealthConsentRepository(
    private val api: AccountApi,
    private val store: HealthConsentStore,
    /** Delivers what is still queued, so the server erases it too. */
    private val beforeWithdraw: suspend () -> Unit = {},
    /** Removes the health data this phone still holds. */
    private val afterWithdraw: suspend () -> Unit = {}
) {
    /** Counts the changes made here, so an older answer of the server never overwrites them. */
    @Volatile private var changes = 0

    fun granted(): Boolean = store.granted()

    /** The person agreed: it counts from now on this phone, and [sync] tells the server. */
    fun grant() {
        changes++
        store.save(granted = true, pending = true)
    }

    suspend fun sync() {
        if (!store.pending()) return
        val before = changes
        if (apiCall { api.healthConsent(HealthConsentRequest(true)) } is ApiResult.Success && before == changes) store.save(granted = true, pending = false)
    }

    /** Adopts what the server holds (given or withdrawn on another phone), unless this phone still has something to tell it. */
    suspend fun refresh() {
        if (store.pending()) return sync()
        val before = changes
        val result = apiCall { api.info() }
        if (result is ApiResult.Success && before == changes) store.save(granted = result.value.healthConsent, pending = false)
    }

    /** Withdraws the consent; the server erases the health data. Needs a connection: it is not done until the server says so. */
    suspend fun withdraw(): ApiResult<Unit> {
        beforeWithdraw()
        val result = apiCall { api.healthConsent(HealthConsentRequest(false)) }
        if (result is ApiResult.Success) {
            changes++
            store.save(granted = false, pending = false)
            afterWithdraw()
        }
        return result
    }

    /** Signing out: the next person on this phone is asked for themselves. */
    fun clear() {
        changes++
        store.save(granted = false, pending = false)
    }
}
