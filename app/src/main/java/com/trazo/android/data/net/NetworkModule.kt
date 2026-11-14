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

/** Reports a 402 so the app can show the plans screen. The response itself still reaches the caller. */
class PaymentRequiredInterceptor(private val events: SessionEvents) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 402) events.emit(SessionEvent.SubscriptionRequired)
        return response
    }
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

class NetworkModule(
    baseUrl: String,
    tokens: TokenStore,
    val events: SessionEvents,
    val gson: Gson = Gson()
) {
    private fun baseClient() = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS)

    private val url = baseUrl

    private fun retrofit(client: OkHttpClient) = Retrofit.Builder().baseUrl(url).client(client)
        .addConverterFactory(GsonConverterFactory.create(gson)).build()

    /** No interceptors: used by the authenticator to refresh without recursion. */
    private val plainAuthApi: AuthApi = retrofit(baseClient().build()).create(AuthApi::class.java)

    val client: OkHttpClient = baseClient()
        .addInterceptor(AuthInterceptor(tokens))
        .addInterceptor(PaymentRequiredInterceptor(events))
        .authenticator(TokenAuthenticator(tokens, plainAuthApi, events))
        .build()

    private val retrofit: Retrofit = retrofit(client)
    fun <T> create(service: Class<T>): T = retrofit.create(service)
}
