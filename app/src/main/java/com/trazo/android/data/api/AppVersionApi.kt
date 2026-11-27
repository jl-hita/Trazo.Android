package com.trazo.android.data.api

import com.trazo.android.data.model.AppVersionPolicy
import retrofit2.Response
import retrofit2.http.GET

/** Public: it is asked before anyone signs in. */
interface AppVersionApi {
    @GET("api/v1/app/version-policy")
    suspend fun versionPolicy(): Response<AppVersionPolicy>
}
