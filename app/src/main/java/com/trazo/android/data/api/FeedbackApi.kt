package com.trazo.android.data.api

import com.trazo.android.data.model.FeedbackRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface FeedbackApi {
    @POST("api/v1/feedback")
    suspend fun send(@Body request: FeedbackRequest): Response<Unit>
}
