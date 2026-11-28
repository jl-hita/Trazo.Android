package com.trazo.android.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

interface ExportApi {
    /** Everything the server holds about the signed-in account, as a JSON file. */
    @GET("api/v1/account/export")
    suspend fun export(): Response<ResponseBody>
}
