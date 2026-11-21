package com.trazo.android.data.net

import com.google.gson.Gson
import com.trazo.android.auth.AuthApi
import com.trazo.android.auth.RefreshRequest
import com.trazo.android.auth.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Adds the bearer token to API calls. Authentication endpoints manage their own credentials. */
class AuthInterceptor(private val tokens: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header("Authorization") != null || request.url.encodedPath.contains("/api/v1/auth/")) return chain.proceed(request)
        val access = tokens.access() ?: return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer $access").build())
    }
}

/**
 * Tells the API who is asking: the language of the app (Spanish unless the device is English) and the time zone of the device, so
 * texts come back in the user's language and "today" is the user's day. Sent on every call, authenticated or not.
 */
class ContextInterceptor(
    private val language: () -> String = { if (java.util.Locale.getDefault().language == "en") "en" else "es" },
    private val zone: () -> String = { java.time.ZoneId.systemDefault().id },
    private val appVersion: () -> String = { com.trazo.android.BuildConfig.VERSION_NAME }
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request().newBuilder().header("Accept-Language", language()).header("X-Time-Zone", zone()).header("X-App-Version", appVersion()).build())
}

/**
 * Refreshes the 60-minute access token when the API answers 401 and retries once. Only one refresh runs at a time; calls
 * that were waiting reuse the new token. A refresh token the server rejects ends the session; a network failure does not.
 */
class TokenAuthenticator(
    private val tokens: TokenStore,
    private val plainAuthApi: AuthApi,
    private val events: SessionEvents
) : Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val sent = response.request.header("Authorization") ?: return null // Anonymous call: a 401 is the answer.
        if (responseCount(response) >= 3) return null
        synchronized(lock) {
            val current = tokens.access()
            if (current != null && "Bearer $current" != sent) {
                return response.request.newBuilder().header("Authorization", "Bearer $current").build()
            }
            val refresh = tokens.refresh()
            if (refresh.isNullOrBlank()) { expire(); return null }
            val result = try {
                runBlocking { plainAuthApi.refresh(RefreshRequest(refresh)) }
            } catch (_: IOException) {
                return null // Offline: keep the session, the caller reports the failure.
            }
            val body = result.body()
            val newAccess = body?.accessToken
            val newRefresh = body?.refreshToken
            if (result.isSuccessful && newAccess != null && newRefresh != null) {
                tokens.save(newAccess, newRefresh)
                return response.request.newBuilder().header("Authorization", "Bearer $newAccess").build()
            }
            if (result.code() in 400..499) expire()
            return null
        }
    }

    private fun expire() {
        tokens.clear()
        events.emit(SessionEvent.SessionExpired)
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        return count
    }
}


/**
 * Temporary, endpoint-scoped diagnostics for the referral invitations crash.
 *
 * Logs the actual response JSON shape before Retrofit/Gson consumes it. Opaque identifiers and
 * account-like values are redacted; field names, JSON types, statuses and timestamps remain visible.
 * The log is local to the device (Logcat), bounded, and never uploaded.
 */
internal fun sanitizedReferralResponseBody(body: String): String {
    val sensitiveKeys = setOf("id", "userid", "code", "link", "email", "name", "token", "accesstoken", "refreshtoken")
    fun sanitize(element: com.google.gson.JsonElement): com.google.gson.JsonElement = when {
        element.isJsonObject -> com.google.gson.JsonObject().also { target ->
            element.asJsonObject.entrySet().forEach { (key, value) ->
                val normalized = key.lowercase().filter { it.isLetterOrDigit() }
                target.add(key, if (normalized in sensitiveKeys) com.google.gson.JsonPrimitive("<redacted>") else sanitize(value))
            }
        }
        element.isJsonArray -> com.google.gson.JsonArray().also { target ->
            element.asJsonArray.forEach { target.add(sanitize(it)) }
        }
        else -> element.deepCopy()
    }
    return try {
        Gson().toJson(sanitize(com.google.gson.JsonParser.parseString(body)))
    } catch (_: Exception) {
        "<non-JSON response; chars=${body.length}>"
    }
}

class ReferralResponseLogInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET" || !request.url.encodedPath.endsWith("/api/v1/referrals/invitations")) {
            return chain.proceed(request)
        }

        val response = chain.proceed(request)
        val body = runCatching { response.peekBody(MAX_LOG_BYTES).string() }.getOrNull()
        val contentLength = response.body?.contentLength() ?: -1L
        val preview = body?.let(::sanitizedReferralResponseBody) ?: "<body unavailable>"
        android.util.Log.i(
            TAG,
            "GET /api/v1/referrals/invitations HTTP ${response.code}; contentType=${response.header("Content-Type")}; " +
                "contentLength=$contentLength; bodyPreview=${preview.take(MAX_LOG_CHARS)}"
        )
        return response
    }

    private companion object {
        const val TAG = "TRAZO_REFERRAL"
        const val MAX_LOG_BYTES = 16L * 1024L
        const val MAX_LOG_CHARS = 8 * 1024
    }
}

class NetworkModule(
    baseUrl: String,
    tokens: TokenStore,
    val events: SessionEvents,
    val gson: Gson = Gson()
) {
    private fun baseClient() = OkHttpClient.Builder()
        .addInterceptor(ContextInterceptor())
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS)

    private val url = baseUrl

    private fun retrofit(client: OkHttpClient) = Retrofit.Builder().baseUrl(url).client(client)
        .addConverterFactory(GsonConverterFactory.create(gson)).build()

    /** No interceptors: used by the authenticator to refresh without recursion. */
    private val plainAuthApi: AuthApi = retrofit(baseClient().build()).create(AuthApi::class.java)

    val client: OkHttpClient = baseClient()
        .addInterceptor(ReferralResponseLogInterceptor())\n        .addInterceptor(AuthInterceptor(tokens))
        .authenticator(TokenAuthenticator(tokens, plainAuthApi, events))
        .build()

    private val retrofit: Retrofit = retrofit(client)
    fun <T> create(service: Class<T>): T = retrofit.create(service)

    /** For public endpoints that must stay anonymous: no bearer token, no refresh. */
    fun <T> createAnonymous(service: Class<T>): T = retrofit(baseClient().build()).create(service)
}
