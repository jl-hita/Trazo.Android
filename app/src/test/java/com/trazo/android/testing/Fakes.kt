package com.trazo.android.testing

import com.trazo.android.auth.*
import com.trazo.android.data.api.PlansApi
import com.trazo.android.data.api.SubscriptionApi
import com.trazo.android.data.model.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException

class MemoryTokenStore(var access: String? = null, var refresh: String? = null) : TokenStore {
    override fun access() = access
    override fun refresh() = refresh
    override fun save(access: String, refresh: String) { this.access = access; this.refresh = refresh }
    override fun clear() { access = null; refresh = null }
}

fun <T> httpError(code: Int, json: String = "{}"): Response<T> = Response.error(code, json.toResponseBody("application/json".toMediaType()))
fun problem(code: Int, title: String): String = """{"title":"$title","status":$code,"detail":"detalle"}"""

class FakePlansApi : PlansApi {
    var weekResponse: () -> Response<WeekView> = { Response.success(sampleWeek()) }
    var planResponse: () -> Response<PlanView> = { Response.success(samplePlan()) }
    var logResponse: (String, LogSessionRequest) -> Response<LogResult> = { _, _ -> Response.success(LogResult(sampleSession(), sampleReport())) }
    var optionsResponse: () -> Response<PlanOptions> = { Response.success(sampleOptions()) }
    var createResponse: (PlanProfileRequest) -> Response<PlanView> = { Response.success(samplePlan()) }
    val logCalls = mutableListOf<Pair<String, LogSessionRequest>>()
    var createdWith: PlanProfileRequest? = null
    var reviewResponse: () -> Response<ReviewRequestView> = { httpError(404, problem(404, "no_review_request")) }
    var reviewedWith: ReviewRequestBody? = null

    override suspend fun options() = optionsResponse()
    override suspend fun create(request: PlanProfileRequest): Response<PlanView> { createdWith = request; return createResponse(request) }
    override suspend fun current() = planResponse()
    override suspend fun week() = weekResponse()
    override suspend fun history(take: Int): Response<List<HistoryItem>> = Response.success(emptyList())
    override suspend fun archive(): Response<Unit> = Response.success(Unit)
    override suspend fun reviewRequest(messages: Boolean) = reviewResponse()
    var sentMessage: ReviewMessageBody? = null
    override suspend fun sendReviewMessage(request: ReviewMessageBody): Response<ReviewRequestView> { sentMessage = request; return Response.success(ReviewRequestView("r1", "in_progress", "2026-11-14T10:00:00Z", messages = listOf(ReviewMessage("m1", "user", request.body, "2026-11-14T10:01:00Z")))) }
    override suspend fun requestReview(request: ReviewRequestBody): Response<ReviewRequestView> { reviewedWith = request; return Response.success(ReviewRequestView("r1", "pending", "2026-11-14T10:00:00Z")) }
    override suspend fun log(sessionId: String, request: LogSessionRequest): Response<LogResult> {
        logCalls += sessionId to request
        return logResponse(sessionId, request)
    }
    override suspend fun alternatives(sessionId: String, exerciseId: String, limit: Int): Response<AlternativesResult> =
        Response.success(AlternativesResult(exerciseId, emptyList(), null))
    override suspend fun swap(sessionId: String, exerciseId: String, request: SwapRequest): Response<SessionView> = Response.success(sampleSession())
}

class FakeSubscriptionApi(var status: () -> Response<SubscriptionStatus> = { Response.success(SubscriptionStatus(hasAccess = true, enforced = false)) }) : SubscriptionApi {
    override suspend fun status() = status.invoke()
    override suspend fun verify(request: VerifySubscriptionRequest) = status.invoke()
}

class FakeAuthApi(var refreshResponse: () -> Response<AuthResponse> = { Response.success(AuthResponse("new-access", "new-refresh")) }) : AuthApi {
    override suspend fun register(request: RegisterRequest): Response<AuthResponse> = TODO()
    override suspend fun login(request: LoginRequest): Response<AuthResponse> = TODO()
    override suspend fun google(request: GoogleLoginRequest): Response<AuthResponse> = TODO()
    override suspend fun verifyTwoFactor(request: TwoFactorVerifyRequest): Response<AuthResponse> = TODO()
    override suspend fun refresh(request: RefreshRequest) = refreshResponse()
    override suspend fun logout(authorization: String, request: RefreshRequest): Response<Unit> = Response.success(Unit)
    override suspend fun me(authorization: String): Response<UserSummary> = Response.success(UserSummary("user-1", "a@b.c", "Ana", "user", false))
    override suspend fun logoutAll(authorization: String): Response<Unit> = Response.success(Unit)
    override suspend fun linkGoogle(authorization: String, request: GoogleLinkRequest): Response<Unit> = Response.success(Unit)
    override suspend fun setupTwoFactor(authorization: String): Response<TwoFactorSetupResponse> = TODO()
    override suspend fun enableTwoFactor(authorization: String, request: TwoFactorCodeRequest): Response<TwoFactorEnableResponse> = TODO()
    override suspend fun disableTwoFactor(authorization: String, request: TwoFactorCodeRequest): Response<Unit> = TODO()
    override suspend fun resendVerification(request: EmailRequest): Response<Unit> = TODO()
    override suspend fun forgotPassword(request: EmailRequest): Response<Unit> = TODO()
    override suspend fun verifyEmail(request: EmailTokenRequest): Response<Unit> = TODO()
    override suspend fun resetPassword(request: ResetPasswordRequest): Response<Unit> = TODO()
}

fun networkFailure(): Nothing = throw IOException("offline")

fun sampleExercise(id: String = "db-bench-press", sets: Int = 3, load: Double? = 22.5) = ExerciseView(
    id, "Press con mancuernas", 1, sets, 8, 12, 2.0, 90, "DoubleProgression", load, null, 1, null, listOf("Chest"), listOf("Triceps"), null
)

fun sampleSession(id: String = "s1", vararg exercises: ExerciseView = arrayOf(sampleExercise())) = SessionView(
    id, 1, "2026-11-16", "Monday", "Tren superior A", 50, "Planned", exercises.toList(), null
)

fun sampleWeek(vararg sessions: SessionView = arrayOf(sampleSession()), end: String = "2099-12-31") = WeekView(
    "plan-1", 1, "2026-11-16", end, "InProgress", MesocycleView(1, "accumulation", false, 1, 4, 1), sessions.toList()
)

fun samplePlan() = PlanView(
    "plan-1", "Active", "InProgress", 1, 12, "2026-11-16", "2027-02-07", "Hypertrophy", "Beginner", "UpperLower",
    listOf("Monday", "Thursday"), 60, emptyList(), emptyList(), emptyList(), 1,
    MacrocycleView("Macrociclo", "", listOf(PhaseView(1, "Base", "accumulation", "2026-11-16", "2026-12-13", "Volumen"))), "No es consejo médico."
)

fun sampleReport() = AdaptationReport(emptyList(), "Hold", "Todo en orden.", emptyList(), emptyList())

fun sampleOptions() = PlanOptions(
    listOf("GeneralFitness", "Hypertrophy", "Strength"), listOf("Beginner", "Intermediate", "Advanced"), listOf("Automatic"),
    listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"), listOf("Knee", "Shoulder"),
    listOf("barbell", "dumbbell"), 2, 6, 35, 180, 4, 52, 12
)

fun logRequest() = LogSessionRequest("Completed", sets = listOf(LogSetRequest("db-bench-press", 1, 22.5, 10, 2.0, true)))
