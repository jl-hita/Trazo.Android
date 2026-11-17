package com.trazo.android.auth

/** Failure of an account operation. The UI turns [code] into text in the user's language. */
class AuthException(val code: AuthError, val http: Int? = null) : Exception(code.name)

enum class AuthError { Generic, IncompleteResponse, NoChallenge, NoSession, TwoFactorSetup, TwoFactorCode, TwoFactorDisable, LogoutAll, LinkGoogle, GoogleEmailInUse, EmailInUse }

class AuthRepository(
    private val api: AuthApi,
    private val store: TokenStore,
    private val referral: com.trazo.android.data.referral.PendingReferralStore? = null
) {
    /** The invitation code that arrived through a link or the Play install referrer and is waiting for a registration. */
    fun pendingReferral(): com.trazo.android.data.referral.PendingReferral? = referral?.peek()

    suspend fun login(email: String, password: String) = consume(api.login(LoginRequest(email.trim(), password)))
    /** [referralCode] is what the person typed; without it the pending code of a link or install referrer goes along. The server decides whether it counts. */
    suspend fun register(email: String, password: String, name: String, referralCode: String? = null): Outcome {
        val r = com.trazo.android.data.referral.referralFor(referralCode, referral?.peek())
        val outcome = consume(api.register(RegisterRequest(email.trim(), password, name.trim(), referralCode = r?.code, referralSource = r?.source)), conflict = AuthError.EmailInUse)
        if (r != null && outcome !is Outcome.Error) referral?.clear()
        return outcome
    }
    suspend fun google(idToken: String): Outcome {
        val r = referral?.peek()
        val outcome = consume(api.google(GoogleLoginRequest(idToken, referralCode = r?.code, referralSource = r?.source)), conflict = AuthError.GoogleEmailInUse)
        if (r != null && outcome !is Outcome.Error) referral?.clear()
        return outcome
    }

    /** Single-use nonce for the next Google ID token. Null when the API has none to give (older server, no connection). */
    suspend fun googleNonce(): String? =
        try { api.googleNonce().takeIf { it.isSuccessful }?.body()?.nonce?.takeIf { it.isNotBlank() } } catch (_: java.io.IOException) { null }
    suspend fun verify(challenge: String, code: String) = consume(api.verifyTwoFactor(TwoFactorVerifyRequest(challenge, code.trim())))

    fun hasStoredSession(): Boolean = store.refresh() != null

    suspend fun restoreSession(): Boolean {
        val token = store.refresh() ?: run { store.clear(); return false }
        return try {
            val response = api.refresh(RefreshRequest(token))
            if (!response.isSuccessful || response.body() == null) {
                if (response.code() in 400..499) store.clear()
                false
            } else { consume(response); store.access() != null }
        } catch (e: Exception) { com.trazo.android.diagnostics.Diagnostics.report(e); false }
    }

    suspend fun currentUser(): UserSummary? {
        val access = store.access() ?: return null
        val response = api.me("Bearer $access")
        if (response.code() == 401) store.clear()
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun logout() {
        val access = store.access()
        val refresh = store.refresh()
        try { if (!access.isNullOrBlank() && !refresh.isNullOrBlank()) api.logout("Bearer $access", RefreshRequest(refresh)) } finally { store.clear() }
    }

    /** Revokes every refresh token of the account (all devices), including this one. */
    suspend fun logoutAll() {
        val response = api.logoutAll(bearer())
        if (!response.isSuccessful) throw AuthException(AuthError.LogoutAll, response.code())
        store.clear()
    }

    /** Links the signed-in account to the Google account that produced [idToken]. */
    suspend fun linkGoogle(idToken: String) {
        val response = api.linkGoogle(bearer(), GoogleLinkRequest(idToken))
        if (!response.isSuccessful) throw AuthException(AuthError.LinkGoogle, response.code())
    }

    suspend fun setupTwoFactor(): TwoFactorSetupResponse {
        val response = api.setupTwoFactor(bearer())
        return response.body() ?: throw AuthException(AuthError.TwoFactorSetup, response.code())
    }

    suspend fun enableTwoFactor(code: String): TwoFactorEnableResponse {
        val response = api.enableTwoFactor(bearer(), TwoFactorCodeRequest(code.trim()))
        return response.body() ?: throw AuthException(AuthError.TwoFactorCode, response.code())
    }

    suspend fun disableTwoFactor(code: String) {
        val response = api.disableTwoFactor(bearer(), TwoFactorCodeRequest(code.trim()))
        if (!response.isSuccessful) throw AuthException(AuthError.TwoFactorDisable, response.code())
    }

    suspend fun resendVerification(email: String) = api.resendVerification(EmailRequest(email.trim()))
    suspend fun forgotPassword(email: String) = api.forgotPassword(EmailRequest(email.trim()))
    suspend fun verifyEmail(token: String) = api.verifyEmail(EmailTokenRequest(token))
    suspend fun resetPassword(token: String, newPassword: String) = api.resetPassword(ResetPasswordRequest(token, newPassword))

    private fun bearer() = "Bearer ${store.access() ?: throw AuthException(AuthError.NoSession)}"

    /**
     * A 409 means "verify your email first" when logging in. For Google sign-in and registration it means the email already
     * belongs to another account, which [conflict] reports instead (a verified account cannot be told to verify anything).
     */
    private fun consume(response: retrofit2.Response<AuthResponse>, conflict: AuthError? = null): Outcome {
        val body = response.body()
        if (response.code() == 409) return if (conflict != null) Outcome.Error(conflict, 409) else Outcome.EmailVerificationRequired(null)
        if (!response.isSuccessful || body == null) return Outcome.Error(AuthError.Generic, response.code())
        if (body.emailVerificationRequired) return Outcome.EmailVerificationRequired(body.message)
        if (body.twoFactorRequired) return body.challengeToken?.let { Outcome.Challenge(it) } ?: Outcome.Error(AuthError.NoChallenge)
        if (body.accessToken.isNullOrBlank() || body.refreshToken.isNullOrBlank()) return Outcome.Error(AuthError.IncompleteResponse)
        store.save(body.accessToken, body.refreshToken)
        return Outcome.Success
    }
}

sealed interface Outcome {
    data object Success : Outcome
    data class Challenge(val token: String) : Outcome
    /** [serverMessage] is the server's own text (may be null; the UI then uses its default). */
    data class EmailVerificationRequired(val serverMessage: String?) : Outcome
    data class Error(val code: AuthError, val http: Int? = null) : Outcome
}
