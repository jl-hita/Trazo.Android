package com.trazo.android.testing

import com.trazo.android.auth.*
import com.trazo.android.data.api.ExercisesApi
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
    /** What the server keeps after a limitations update, so the plan read afterwards shows it. */
    var storedLimitations: List<String> = emptyList()
    var planResponse: () -> Response<PlanView> = { Response.success(samplePlan().copy(limitations = storedLimitations)) }
    var logResponse: (String, LogSessionRequest) -> Response<LogResult> = { _, _ -> Response.success(LogResult(sampleSession(), sampleReport())) }
    var optionsResponse: () -> Response<PlanOptions> = { Response.success(sampleOptions()) }
    var createResponse: (PlanProfileRequest) -> Response<PlanView> = { Response.success(samplePlan()) }
    val logCalls = mutableListOf<Pair<String, LogSessionRequest>>()
    var createdWith: PlanProfileRequest? = null
    var manualResponse: (ManualPlanRequest) -> Response<PlanView> = { Response.success(samplePlan()) }
    var manualCreatedWith: ManualPlanRequest? = null
    var reviewResponse: () -> Response<ReviewRequestView> = { httpError(404, problem(404, "no_review_request")) }
    var reviewedWith: ReviewRequestBody? = null

    override suspend fun options() = optionsResponse()
    override suspend fun create(request: PlanProfileRequest): Response<PlanView> { createdWith = request; return createResponse(request) }
    override suspend fun createManual(request: ManualPlanRequest): Response<PlanView> { manualCreatedWith = request; return manualResponse(request) }
    override suspend fun current() = planResponse()
    override suspend fun week() = weekResponse()
    var loadResponse: () -> Response<PlanLoadView> = { Response.success(PlanLoadView("p1", 4, 2, "sets", listOf(
        WeekLoadView(1, "2026-11-09", "accumulation", false, 20, 18, 2, 2), WeekLoadView(2, "2026-11-16", "accumulation", false, 22, 5, 2, 1),
        WeekLoadView(3, "2026-11-23", "accumulation", false, 22, null, 2, 0), WeekLoadView(4, "2026-11-30", "deload", true, null, null, 0, 0)))) }
    override suspend fun updateManual(request: ManualPlanRequest): Response<PlanView> = Response.error(404, okhttp3.ResponseBody.create(null, ""))
    val sessionEdits = mutableListOf<Pair<String, SessionEditRequest>>()
    override suspend fun editSession(sessionId: String, request: SessionEditRequest): Response<SessionView> {
        sessionEdits += sessionId to request
        return Response.success(sampleSession(sessionId).copy(customised = true))
    }
    var adjustmentsCleared = 0
    override suspend fun blockSummaries(): Response<BlockSummaries> = Response.success(BlockSummaries(
        listOf(1, 2).map { BlockSummary(it, false, it * 4 - 3, 4, 12, 10, listOf(BlockMuscle("Chest", 40.0, 36.0)), emptyList()) }))
    val program = ProgramView("full-body-3", "Full body", "x", "Beginner", 3, "Calendar", "FullBody", 60, 12)
    var programCreated: Pair<String, PlanProfileRequest>? = null
    override suspend fun programs(): Response<List<ProgramView>> = Response.success(listOf(program))
    override suspend fun programSheet(id: String, request: PlanProfileRequest): Response<ProgramSheetView> =
        Response.success(ProgramSheetView(program, listOf("dumbbell"), listOf(ProgramMuscleSets("Chest", 8.0)), ProgramSampleSession("A", 55, emptyList())))
    override suspend fun createProgram(id: String, request: PlanProfileRequest): Response<PlanView> { programCreated = id to request; return Response.success(samplePlan()) }
    override suspend fun clearAdjustments(): Response<Unit> { adjustmentsCleared++; return Response.success(204, Unit) }
    override suspend fun load() = loadResponse()
    override suspend fun block(): Response<BlockView> = Response.error(404, okhttp3.ResponseBody.create(null, ""))
    var prioritiesResponse: (Map<String, String>) -> Response<PlanView> = { Response.success(samplePlan().copy(musclePriorities = it)) }
    val prioritiesSent = mutableListOf<Map<String, String>>()
    override suspend fun updateMusclePriorities(request: UpdateMusclePrioritiesRequest): Response<PlanView> {
        prioritiesSent += request.musclePriorities; return prioritiesResponse(request.musclePriorities)
    }
    var limitationsResponse: (List<String>) -> Response<PlanView> = { Response.success(samplePlan().copy(limitations = it)) }
    val limitationsSent = mutableListOf<List<String>>()
    override suspend fun updateLimitations(request: UpdateLimitationsRequest): Response<PlanView> {
        limitationsSent += request.limitations; storedLimitations = request.limitations; return limitationsResponse(request.limitations)
    }
    var rescheduleResponse: () -> Response<RescheduleResult> = { Response.success(RescheduleResult(1, 8, "2026-11-16", "2027-02-07")) }
    var rescheduleCalls = 0
    override suspend fun reschedule(): Response<RescheduleResult> { rescheduleCalls++; return rescheduleResponse() }
    var adaptationsResponse: () -> Response<AdaptationHistoryView> = { Response.success(AdaptationHistoryView()) }
    override suspend fun adaptations(take: Int, scope: String): Response<AdaptationHistoryView> = adaptationsResponse()
    var skipResponse: () -> Response<SkipResult> = { Response.success(SkipResult(sampleSession(), false, "ok")) }
    val skipped = mutableListOf<Pair<String, SkipSessionRequest>>()
    override suspend fun skip(sessionId: String, request: SkipSessionRequest): Response<SkipResult> { skipped += sessionId to request; return skipResponse() }
    var nextResponse: () -> Response<NextSessionView> = { Response.success(sampleNext()) }
    override suspend fun next(): Response<NextSessionView> = nextResponse()
    override suspend fun progress(): Response<ProgressView> = Response.error(404, okhttp3.ResponseBody.create(null, ""))
    override suspend fun history(take: Int): Response<List<HistoryItem>> = Response.success(emptyList())
    override suspend fun archive(): Response<Unit> = Response.success(Unit)
    var messagesResponse: (() -> Response<ReviewRequestView>)? = null
    override suspend fun reviewRequest(messages: Boolean) = if (messages && messagesResponse != null) messagesResponse!!() else reviewResponse()
    var sentMessage: ReviewMessageBody? = null
    var sendWithoutMessages = false
    override suspend fun sendReviewMessage(request: ReviewMessageBody): Response<ReviewRequestView> { sentMessage = request; return Response.success(ReviewRequestView("r1", "in_progress", "2026-11-14T10:00:00Z", messages = if (sendWithoutMessages) null else listOf(ReviewMessage("m1", "user", request.body, "2026-11-14T10:01:00Z")))) }
    override suspend fun requestReview(request: ReviewRequestBody): Response<ReviewRequestView> { reviewedWith = request; return Response.success(ReviewRequestView("r1", "pending", "2026-11-14T10:00:00Z")) }
    override suspend fun log(sessionId: String, request: LogSessionRequest): Response<LogResult> {
        logCalls += sessionId to request
        return logResponse(sessionId, request)
    }
    override suspend fun alternatives(sessionId: String, exerciseId: String, limit: Int): Response<AlternativesResult> =
        Response.success(AlternativesResult(exerciseId, emptyList(), null))
    override suspend fun swap(sessionId: String, exerciseId: String, request: SwapRequest): Response<SessionView> = Response.success(sampleSession())
    override suspend fun addExercise(sessionId: String, request: AddExerciseRequest): Response<SessionView> = Response.success(
        sampleSession(sessionId, sampleExercise(), sampleExercise().copy(exerciseId = request.exerciseId, order = 2, sets = 3, isExtra = true))
    )
}

class FakeSubscriptionApi(var status: () -> Response<SubscriptionStatus> = { Response.success(SubscriptionStatus(hasAccess = true, enforced = false)) }) : SubscriptionApi {
    override suspend fun status() = status.invoke()
    override suspend fun verify(request: VerifySubscriptionRequest) = status.invoke()
}

class FakeAuthApi(var refreshResponse: () -> Response<AuthResponse> = { Response.success(AuthResponse("new-access", "new-refresh")) }) : AuthApi {
    override suspend fun register(request: RegisterRequest): Response<AuthResponse> = TODO()
    override suspend fun login(request: LoginRequest): Response<AuthResponse> = TODO()
    override suspend fun google(request: GoogleLoginRequest): Response<AuthResponse> = TODO()
    override suspend fun googleNonce(): Response<GoogleNonceResponse> = Response.success(GoogleNonceResponse("test-nonce"))
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
    override suspend fun setupPassword(authorization: String): Response<Unit> = Response.success(Unit)
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

fun sampleNext(session: SessionView? = sampleSession(), availability: AvailabilityView? = null) = NextSessionView(
    "plan-1", "Calendar", "InProgress", session, NextPositionView(1, 1, 2, null), availability
)

fun sampleReport() = AdaptationReport(emptyList(), "Hold", "Todo en orden.", emptyList(), emptyList())

fun sampleOptions() = PlanOptions(
    listOf("GeneralFitness", "Hypertrophy", "Strength"), listOf("Beginner", "Intermediate", "Advanced"), listOf("Automatic"),
    listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"), listOf("Knee", "Shoulder"),
    listOf("barbell", "dumbbell"), 2, 6, 35, 180, 4, 52, 12
)

fun logRequest() = LogSessionRequest("Completed", sets = listOf(LogSetRequest("db-bench-press", 1, 22.5, 10, 2.0, true)))

class FakeExercisesApi : ExercisesApi {
    private val item = ExerciseListItem("db-bench-press", "Press banca con mancuernas", "Dumbbell bench press", "", "Beginner", "dumbbell", listOf("dumbbell"), "HorizontalPush", listOf("Chest"), emptyList(), "Beginner", false, false, 90)
    var detailResponse: Response<ExerciseDetail> = Response.success(ExerciseDetail(
        "db-bench-press", "Press de banca", "Bench press", "Descripción en español.", listOf("Coloca las manos."), listOf("Controla la bajada."),
        "Beginner", "barbell", listOf("barbell"), "Chest", "HorizontalPush", listOf("Chest"), emptyList(), "Beginner"
    ))
    var localizedResponse: Response<LocalizedExerciseText> = Response.success(LocalizedExerciseText(
        "en", "Bench press", "A chest exercise.", listOf("Set your hands."), listOf("Control the descent."),
        "en", "en", "en", "en", true, emptyList()
    ))
    override suspend fun search(q: String?, movementPattern: String?, difficulty: String?, experience: String?, equipment: String?, muscle: String?, page: Int, pageSize: Int): Response<ExercisePage> =
        Response.success(ExercisePage(listOf(item), page, pageSize, 1, 1))
    override suspend fun detail(id: String): Response<ExerciseDetail> = detailResponse
    override suspend fun localizedDetail(id: String): Response<LocalizedExerciseText> = localizedResponse
    override suspend fun images(id: String): Response<ExerciseImageSlots> = TODO()
}
