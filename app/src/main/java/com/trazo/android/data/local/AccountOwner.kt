package com.trazo.android.data.local

import android.content.Context
import com.trazo.android.auth.TokenStore

/** Remembers whose unsent workouts and drafts are on the phone. */
interface OwnerStore {
    fun get(): String?
    fun set(id: String?)
}

class SharedPreferencesOwnerStore(context: Context) : OwnerStore {
    private val prefs = context.applicationContext.getSharedPreferences("trazo_owner", Context.MODE_PRIVATE)
    override fun get(): String? = prefs.getString("id", null)
    override fun set(id: String?) { prefs.edit().apply { if (id == null) remove("id") else putString("id", id) }.apply() }
}

/**
 * A session that expires keeps the unsent workouts, so they must never reach another account: before the tokens of a sign-in
 * are stored (and so before anything can be sent with them) the data of a different, or unknown, owner is wiped.
 */
class OwnedTokenStore(
    private val tokens: TokenStore,
    private val owner: OwnerStore,
    private val identify: (String) -> String? = ::subjectOf,
    private val wipe: () -> Unit
) : TokenStore by tokens {
    override fun save(access: String, refresh: String) {
        val id = identify(access)
        val previous = owner.get()
        // A renewal of the running session keeps the same subject: nothing to do. No owner yet: data left by an earlier
        // version belongs to the session that is already open, and is only wiped if there was none.
        if (previous != null && previous != id || previous == null && tokens.refresh() == null) wipe()
        owner.set(id)
        tokens.save(access, refresh)
    }

    override fun clear() {
        // A session of an earlier version may end before it is ever renewed: note whose it was.
        if (owner.get() == null) runCatching { tokens.access()?.let(identify) }.getOrNull()?.let(owner::set)
        tokens.clear()
    }
}

/** The account a JWT access token belongs to (its "sub" claim), or null when it cannot be read. Not a validation. */
fun subjectOf(accessToken: String): String? = runCatching {
    val payload = java.util.Base64.getUrlDecoder().decode(accessToken.split('.')[1]).toString(Charsets.UTF_8)
    com.google.gson.JsonParser.parseString(payload).asJsonObject.get("sub")?.asString
}.getOrNull()
