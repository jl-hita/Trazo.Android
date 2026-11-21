package com.trazo.android.data.referral

import android.content.Context
import android.net.Uri
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.apiCall
import com.trazo.android.data.net.apiCallWithBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// Contracts of the referral programme (backend: /api/v1/referrals). The app only shows what the server decided: who is attributed,
// who paid and what was earned are never computed here.

data class ReferralStats(
    val invited: Int = 0, val pending: Int = 0, val converted: Int = 0, val rewards: Int = 0,
    val daysEarned: Int = 0, val daysPending: Int = 0, val daysDeferred: Int = 0
)

/** [enabled] false: the programme is off (no code, no link). [link] is the personal invitation link. */
data class ReferralMe(
    val enabled: Boolean = false, val rewardDays: Int = 30, val code: String? = null, val link: String? = null, val stats: ReferralStats? = null
)

/** One invitation: no name, e-mail or identity of the invited person, only what the inviter needs to follow their reward. */
data class ReferralInvitation(
    val id: String, val registeredAt: String, val status: String, val convertedAt: String? = null,
    val plan: String? = null, val rewardDays: Int? = null, val rewardState: String? = null
)

data class ReferralInvitations(val items: List<ReferralInvitation> = emptyList())

data class PremiumGrant(
    val id: String, val kind: String, val days: Int, val status: String, val grantedAt: String,
    val startsAt: String? = null, val endsAt: String? = null, val googleExpiryAfter: String? = null
)

data class PremiumGrants(val items: List<PremiumGrant> = emptyList())

data class SharedRequest(val channel: String? = null)
data class InstallReferrerRequest(val code: String)
data class InstallCampaignRequest(val campaign: String)
data class ClientEventRequest(val name: String)

interface ReferralApi {
    @GET("api/v1/referrals/me") suspend fun me(): Response<ReferralMe>
    @GET("api/v1/referrals/invitations") suspend fun invitations(@Query("status") status: String? = null): Response<ReferralInvitations>
    @GET("api/v1/referrals/rewards") suspend fun rewards(): Response<PremiumGrants>
    @POST("api/v1/referrals/shared") suspend fun shared(@Body request: SharedRequest): Response<Unit>
    @POST("api/v1/analytics/events") suspend fun event(@Body request: ClientEventRequest): Response<Unit>
}

/** Anonymous: the install referrer is read before there is an account. */
interface ReferralAnonymousApi {
    @POST("api/v1/referrals/install-referrer") suspend fun installReferrer(@Body request: InstallReferrerRequest): Response<Unit>
    @POST("api/v1/campaigns/install-referrer") suspend fun campaignInstallReferrer(@Body request: InstallCampaignRequest): Response<Unit>
}

/** The programme's rules that the app needs to know to read and keep a code. The server validates everything again. */
object ReferralCodes {
    const val LENGTH = 8
    /** No 0/O/1/I/L: the codes are read aloud and typed by hand. */
    const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    const val REFERRER_KEY = "trazo_ref"

    /** Upper-cases and strips spaces and dashes; null when the text cannot be a code. */
    fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank() || raw.length > 20) return null
        val code = raw.filter { it != ' ' && it != '-' && it != '_' }.uppercase()
        return code.takeIf { it.length == LENGTH && it.all { c -> c in ALPHABET } }
    }

    /** The code of an invitation link (`https://host/invite/CODE`), or null for any other address. */
    fun fromLink(uri: Uri?): String? = fromPath(uri?.pathSegments)

    /** Same rule on the path segments (testable without Android): exactly `invite/CODE`. */
    fun fromPath(segments: List<String>?): String? {
        if (segments == null) return null
        return if (segments.size == 2 && segments[0] == "invite") normalize(segments[1]) else null
    }

    /** The code inside the Play Install Referrer string (`trazo_ref=CODE`, possibly among other parameters). */
    fun fromInstallReferrer(referrer: String?): String? {
        if (referrer.isNullOrBlank() || referrer.length > 500) return null
        val value = referrer.split('&').map { it.split('=', limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == REFERRER_KEY }?.get(1)
        return normalize(value?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrNull() })
    }
}

/** Marketing campaign links (backend: /c/{slug}). The slug travels in the Play Install Referrer as  and is sent back at registration. */
object CampaignCodes {
    const val REFERRER_KEY = "trazo_cmp"
    private val SLUG = Regex("^[a-z0-9][a-z0-9-]{1,30}[a-z0-9]$")

    /** Lower-cases and checks the link name; null when the text cannot be one. The server validates it again. */
    fun normalize(raw: String?): String? = raw?.trim()?.lowercase()?.takeIf { it.length <= 32 && SLUG.matches(it) }

    /** The campaign inside the Play Install Referrer string (trazo_cmp=slug, among the utm_* parameters Google Play also understands). */
    fun fromInstallReferrer(referrer: String?): String? {
        if (referrer.isNullOrBlank() || referrer.length > 500) return null
        val value = referrer.split('&').map { it.split('=', limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == REFERRER_KEY }?.get(1)
        return normalize(value?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrNull() })
    }
}

object ReferralSource {
    const val LINK = "link"
    const val MANUAL = "manual"
    const val INSTALL_REFERRER = "install_referrer"
}

data class PendingReferral(val code: String, val source: String)

/** The invitation code waiting for the registration. The FIRST code wins: opening more links must not replace it. */
interface PendingReferralStore {
    fun peek(): PendingReferral?
    fun save(code: String, source: String)
    fun clear()

    /** The marketing campaign the install came from, waiting for the registration. Like the invitation, the FIRST one wins. */
    fun peekCampaign(): String? = null
    fun saveCampaign(slug: String) {}
    fun clearCampaign() {}
}

class SharedPreferencesPendingReferralStore(context: Context) : PendingReferralStore {
    private val prefs = context.applicationContext.getSharedPreferences("referral", Context.MODE_PRIVATE)
    override fun peek(): PendingReferral? {
        val code = ReferralCodes.normalize(prefs.getString("code", null)) ?: return null
        return PendingReferral(code, prefs.getString("source", null) ?: ReferralSource.MANUAL)
    }
    override fun save(code: String, source: String) {
        val normalized = ReferralCodes.normalize(code) ?: return
        if (peek() != null) return
        prefs.edit().putString("code", normalized).putString("source", source).apply()
    }
    override fun clear() { prefs.edit().remove("code").remove("source").apply() }

    override fun peekCampaign(): String? = CampaignCodes.normalize(prefs.getString("campaign", null))
    override fun saveCampaign(slug: String) {
        val normalized = CampaignCodes.normalize(slug) ?: return
        if (peekCampaign() != null) return
        prefs.edit().putString("campaign", normalized).apply()
    }
    override fun clearCampaign() { prefs.edit().remove("campaign").apply() }

    /** True once the Play Install Referrer has been read (it is only worth asking on the first launch). */
    fun installReferrerChecked(): Boolean = prefs.getBoolean("install_checked", false)
    fun markInstallReferrerChecked() { prefs.edit().putBoolean("install_checked", true).apply() }
}

/** Which code and source go with a registration: what the user typed (manual) or the pending one (link or install referrer). */
fun referralFor(typed: String?, pending: PendingReferral?): PendingReferral? {
    val typedCode = ReferralCodes.normalize(typed)
    return when {
        typedCode != null && pending != null && typedCode == pending.code -> pending
        typedCode != null -> PendingReferral(typedCode, ReferralSource.MANUAL)
        else -> pending
    }
}

class ReferralRepository(private val api: ReferralApi) {
    suspend fun me(): ApiResult<ReferralMe> = apiCallWithBody("GET /api/v1/referrals/me") { api.me() }
    suspend fun invitations(): ApiResult<List<ReferralInvitation>> = apiCallWithBody("GET /api/v1/referrals/invitations") { api.invitations() }.let {
        when (it) { is ApiResult.Success -> ApiResult.Success(it.value.items); is ApiResult.Failure -> it }
    }
    suspend fun rewards(): ApiResult<List<PremiumGrant>> = apiCallWithBody("GET /api/v1/referrals/rewards") { api.rewards() }.let {
        when (it) { is ApiResult.Success -> ApiResult.Success(it.value.items); is ApiResult.Failure -> it }
    }

    /** Best effort counter: the share sheet was opened. The app cannot know whether anyone received the invitation. */
    suspend fun shared(channel: String? = null) { apiCall { api.shared(SharedRequest(channel)) } }

    /** Usage milestone (never money): reported once per account by the server. */
    suspend fun onboardingCompleted() { apiCall { api.event(ClientEventRequest("onboarding_completed")) } }
}

/** What to send as the invitation message. The text is translated by the caller; this only joins it with the link. */
fun invitationMessage(template: String, link: String): String = template.replace("{link}", link)
