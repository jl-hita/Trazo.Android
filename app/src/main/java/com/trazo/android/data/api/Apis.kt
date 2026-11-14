package com.trazo.android.data.api

import com.trazo.android.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PlansApi {
    @GET("api/v1/plans/options") suspend fun options(): Response<PlanOptions>
    @POST("api/v1/plans") suspend fun create(@Body request: PlanProfileRequest): Response<PlanView>
    @GET("api/v1/plans/current") suspend fun current(): Response<PlanView>
    @GET("api/v1/plans/current/week") suspend fun week(): Response<WeekView>
    @GET("api/v1/plans/current/history") suspend fun history(@Query("take") take: Int = 30): Response<List<HistoryItem>>
    @POST("api/v1/plans/current/archive") suspend fun archive(): Response<Unit>
    @POST("api/v1/plans/current/sessions/{sessionId}/log")
    suspend fun log(@Path("sessionId") sessionId: String, @Body request: LogSessionRequest): Response<LogResult>
    @GET("api/v1/plans/current/sessions/{sessionId}/exercises/{exerciseId}/alternatives")
    suspend fun alternatives(@Path("sessionId") sessionId: String, @Path("exerciseId") exerciseId: String, @Query("limit") limit: Int = 5): Response<AlternativesResult>
    @POST("api/v1/plans/current/sessions/{sessionId}/exercises/{exerciseId}/swap")
    suspend fun swap(@Path("sessionId") sessionId: String, @Path("exerciseId") exerciseId: String, @Body request: SwapRequest): Response<SessionView>
}

interface ExercisesApi {
    @GET("api/v1/exercises")
    suspend fun search(
        @Query("q") q: String? = null,
        @Query("movementPattern") movementPattern: String? = null,
        @Query("difficulty") difficulty: String? = null,
        @Query("experience") experience: String? = null,
        @Query("equipment") equipment: String? = null,
        @Query("muscle") muscle: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 30
    ): Response<ExercisePage>

    @GET("api/v1/exercises/{id}") suspend fun detail(@Path("id") id: String): Response<ExerciseDetail>
    @GET("api/v1/exercises/{id}/images") suspend fun images(@Path("id") id: String): Response<ExerciseImageSlots>
}

interface SubscriptionApi {
    @GET("api/v1/subscription") suspend fun status(): Response<SubscriptionStatus>
    @POST("api/v1/subscription/verify") suspend fun verify(@Body request: VerifySubscriptionRequest): Response<SubscriptionStatus>
}
