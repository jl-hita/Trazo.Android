package com.trazo.android.push

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

/** Firebase options for this install. Nothing in it is secret; the superadmin edits it in the console's Settings. */
data class PushClientConfig(
    val enabled: Boolean = false,
    val projectId: String? = null,
    val applicationId: String? = null,
    val apiKey: String? = null,
    val senderId: String? = null
) {
    /** All the values Firebase needs to start; anything less means push is off. */
    val usable: Boolean get() = enabled && !projectId.isNullOrBlank() && !applicationId.isNullOrBlank() && !apiKey.isNullOrBlank()
}

data class RegisterDeviceBody(val token: String, val platform: String = "android")
data class UnregisterDeviceBody(val token: String)

interface PushApi {
    @GET("api/v1/push/config") suspend fun config(): Response<PushClientConfig>
    @PUT("api/v1/devices") suspend fun register(@Body body: RegisterDeviceBody): Response<Unit>
    @POST("api/v1/devices/unregister") suspend fun unregister(@Body body: UnregisterDeviceBody): Response<Unit>
}
