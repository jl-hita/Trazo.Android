package com.trazo.android.data.repo

import com.trazo.android.data.api.AccountApi
import com.trazo.android.data.api.LanguageRequest
import com.trazo.android.data.local.AppLocale
import com.trazo.android.data.local.LanguageChoice
import com.trazo.android.data.local.LanguageStore
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.apiCall

/**
 * The language of the app is a choice of the user: it is kept on the device (it must work before signing in and offline) and
 * told to the server, so the e-mails and notifications TRAZO sends use it too. The server also gives it back when the user signs in
 * on another device, and that choice is adopted here.
 */
class LanguageRepository(private val api: AccountApi, private val store: LanguageStore) {
    fun choice(): String = store.choice()

    /** Saves the choice and puts it into effect now; the server is told afterwards (best effort: the choice stands even if it fails). */
    suspend fun choose(choice: String): ApiResult<Unit> {
        val normalized = LanguageChoice.normalize(choice)
        store.save(normalized)
        AppLocale.apply(normalized)
        return apiCall { api.language(LanguageRequest(normalized)) }
    }

    /**
     * The choice the server holds for the signed-in user ("es", "en", or null for automatic). When it differs from this device's it is
     * adopted, and true is returned so the screen can be rebuilt in that language. A user who never chose has null: nothing changes.
     */
    fun adopt(serverChoice: String?): Boolean {
        if (serverChoice == null) return false
        val normalized = LanguageChoice.normalize(serverChoice)
        if (normalized == store.choice()) return false
        store.save(normalized)
        AppLocale.apply(normalized)
        return true
    }
}
