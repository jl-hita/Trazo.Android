package com.trazo.android.data.api

import com.trazo.android.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PlansApi {
    @GET("api/v1/plans/options") suspend fun options(): Response<PlanOptions>
    @POST("api/v1/plans") suspend fun create(@Body request: PlanProfileRequest): Response<PlanView>
    @POST("api/v1/plans/manual") suspend fun createManual(@Body request: ManualPlanRequest): Response<PlanView>
    @GET("api/v1/plans/programs") suspend fun programs(): Response<List<ProgramView>>
    @POST("api/v1/plans/programs/{id}/sheet")
    suspend fun programSheet(@Path("id") id: String, @Body request: PlanProfileRequest): Response<ProgramSheetView>
    @POST("api/v1/plans/programs/{id}")
    suspend fun createProgram(@Path("id") id: String, @Body request: PlanProfileRequest): Response<PlanView>
    @PUT("api/v1/plans/current/manual") suspend fun updateManual(@Body request: ManualPlanRequest): Response<PlanView>
    @GET("api/v1/plans/current") suspend fun current(): Response<PlanView>
    @GET("api/v1/plans/current/week") suspend fun week(): Response<WeekView>
    @GET("api/v1/plans/current/load") suspend fun load(): Response<PlanLoadView>
    @GET("api/v1/plans/current/block") suspend fun block(): Response<BlockView>
    @GET("api/v1/plans/current/next") suspend fun next(): Response<NextSessionView>
    @GET("api/v1/plans/progress") suspend fun progress(): Response<ProgressView>
    @GET("api/v1/plans/current/blocks/summary") suspend fun blockSummaries(): Response<BlockSummaries>
    @GET("api/v1/plans/current/recommendations") suspend fun recommendations(): Response<Recommendations>
    @GET("api/v1/plans/current/adaptations") suspend fun adaptations(@Query("take") take: Int = 50, @Query("scope") scope: String = "block"): Response<AdaptationHistoryView>
    @GET("api/v1/plans/current/history") suspend fun history(@Query("take") take: Int = 30): Response<List<HistoryItem>>
    @GET("api/v1/plans/current/review-request") suspend fun reviewRequest(@Query("messages") messages: Boolean = false): Response<ReviewRequestView>
    @POST("api/v1/plans/current/review-request") suspend fun requestReview(@Body request: ReviewRequestBody): Response<ReviewRequestView>
    @POST("api/v1/plans/current/review-request/messages") suspend fun sendReviewMessage(@Body request: ReviewMessageBody): Response<ReviewRequestView>
    @PUT("api/v1/plans/current/limitations") suspend fun updateLimitations(@Body request: UpdateLimitationsRequest): Response<PlanView>
    @PUT("api/v1/plans/current/muscle-priorities") suspend fun updateMusclePriorities(@Body request: UpdateMusclePrioritiesRequest): Response<PlanView>
    @POST("api/v1/plans/current/reschedule") suspend fun reschedule(): Response<RescheduleResult>
    @POST("api/v1/plans/current/sessions/{sessionId}/skip")
    suspend fun skip(@Path("sessionId") sessionId: String, @Body request: SkipSessionRequest): Response<SkipResult>
    @POST("api/v1/plans/current/archive") suspend fun archive(): Response<Unit>
    @POST("api/v1/plans/current/sessions/{sessionId}/log")
    suspend fun log(@Path("sessionId") sessionId: String, @Body request: LogSessionRequest): Response<LogResult>
    @GET("api/v1/plans/current/sessions/{sessionId}/exercises/{exerciseId}/alternatives")
    suspend fun alternatives(@Path("sessionId") sessionId: String, @Path("exerciseId") exerciseId: String, @Query("limit") limit: Int = 5): Response<AlternativesResult>
    @PUT("api/v1/plans/current/sessions/{sessionId}/exercises")
    suspend fun editSession(@Path("sessionId") sessionId: String, @Body request: SessionEditRequest): Response<SessionView>
    @PUT("api/v1/plans/current/session-minutes") suspend fun sessionMinutes(@Body request: SessionMinutesRequest): Response<PlanView>
    @DELETE("api/v1/plans/current/adjustments") suspend fun clearAdjustments(): Response<Unit>
    @POST("api/v1/plans/current/sessions/{sessionId}/exercises/{exerciseId}/swap")
    suspend fun swap(@Path("sessionId") sessionId: String, @Path("exerciseId") exerciseId: String, @Body request: SwapRequest): Response<SessionView>
    @POST("api/v1/plans/current/sessions/{sessionId}/exercises")
    suspend fun addExercise(@Path("sessionId") sessionId: String, @Body request: AddExerciseRequest): Response<SessionView>
}

data class LanguageRequest(val language: String?)

/** The account of the signed-in user. */
interface AccountApi {
    /** "es", "en" or "auto" (follow the language of each request). */
    @PUT("api/v1/account/language") suspend fun language(@Body request: LanguageRequest): Response<Unit>
    @GET("api/v1/account") suspend fun info(): Response<com.trazo.android.data.model.AccountInfo>
    /** Gives the explicit consent for health data, or withdraws it (the server erases that data). */
    @PUT("api/v1/account/health-consent") suspend fun healthConsent(@Body request: com.trazo.android.data.model.HealthConsentRequest): Response<Unit>
    /** Optional and revocable: whether the training data may be used to study and improve the plans. */
    @PUT("api/v1/account/research-consent") suspend fun researchConsent(@Body request: com.trazo.android.data.model.ResearchConsentRequest): Response<Unit>
    /** POST rather than DELETE: the request carries the re-authentication in its body. */
    @POST("api/v1/account/delete") suspend fun delete(@Body request: com.trazo.android.data.model.DeleteAccountRequest): Response<com.trazo.android.data.model.DeleteAccountResult>
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
    @GET("api/v1/exercises/{id}/localized") suspend fun localizedDetail(@Path("id") id: String): Response<LocalizedExerciseText>
    @GET("api/v1/exercises/{id}/images") suspend fun images(@Path("id") id: String): Response<ExerciseImageSlots>
}

/** Anonymous product counters: the backend only adds one to the day's total for the place, it stores nothing about the user. */
interface AnalyticsApi {
    @POST("api/v1/analytics/paywall-view") suspend fun paywallView(@Body request: PaywallViewRequest): Response<Unit>
}

interface SubscriptionApi {
    @GET("api/v1/subscription") suspend fun status(): Response<SubscriptionStatus>
    @POST("api/v1/subscription/verify") suspend fun verify(@Body request: VerifySubscriptionRequest): Response<SubscriptionStatus>
}
