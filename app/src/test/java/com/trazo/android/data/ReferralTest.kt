package com.trazo.android.data

import com.google.gson.Gson
import com.trazo.android.auth.*
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.ContextInterceptor
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.data.referral.*
import com.trazo.android.testing.FakeAuthApi
import com.trazo.android.testing.MemoryTokenStore
import com.trazo.android.testing.httpError
import com.trazo.android.testing.networkFailure
import com.trazo.android.ui.referral.ReferralViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

private class MemoryReferralStore : PendingReferralStore {
    var value: PendingReferral? = null
    override fun peek() = value
    override fun save(code: String, source: String) {
        val normalized = ReferralCodes.normalize(code) ?: return
        if (value == null) value = PendingReferral(normalized, source)
    }
    override fun clear() { value = null }
    var campaign: String? = null
    override fun peekCampaign() = campaign
    override fun saveCampaign(slug: String) { val s = CampaignCodes.normalize(slug) ?: return; if (campaign == null) campaign = s }
    override fun clearCampaign() { campaign = null }
}

/** Records what registration and Google sign-in send; everything else behaves like the shared fake. */
private class RecordingAuthApi(private val base: AuthApi = FakeAuthApi(), private val fail: Boolean = false) : AuthApi by base {
    var registered: RegisterRequest? = null
    var googled: GoogleLoginRequest? = null
    override suspend fun register(request: RegisterRequest): Response<AuthResponse> {
        registered = request
        return if (fail) httpError(500) else Response.success(AuthResponse(emailVerificationRequired = true))
    }
    override suspend fun google(request: GoogleLoginRequest): Response<AuthResponse> {
        googled = request
        return Response.success(AuthResponse("a", "r"))
    }
}

class ReferralCodesTest {
    @Test fun `codes are normalised like the server does`() {
        assertEquals("ABCD2345", ReferralCodes.normalize(" abcd-2345 "))
        assertEquals("ABCD2345", ReferralCodes.normalize("abcd 2345"))
    }

    @Test fun `text that cannot be a code is rejected`() {
        listOf(null, "", "short", "ABCD0OIL", "ABCDEFGHJ", "'; DROP TABLE users;--", "A".repeat(500)).forEach { assertNull(it, ReferralCodes.normalize(it)) }
    }

    @Test fun `the code of an invitation link is read from the path only`() {
        assertEquals("ABCD2345", ReferralCodes.fromPath(listOf("invite", "abcd2345")))
        assertNull(ReferralCodes.fromPath(listOf("account", "verify-email")))
        assertNull(ReferralCodes.fromPath(listOf("invite")))
        assertNull(ReferralCodes.fromPath(listOf("invite", "ABCD2345", "extra")))
        assertNull(ReferralCodes.fromPath(null))
    }

    @Test fun `the Play install referrer is parsed among other parameters`() {
        assertEquals("ABCD2345", ReferralCodes.fromInstallReferrer("trazo_ref=ABCD2345"))
        assertEquals("ABCD2345", ReferralCodes.fromInstallReferrer("utm_source=google-play&trazo_ref=abcd2345&utm_medium=organic"))
        assertEquals("ABCD2345", ReferralCodes.fromInstallReferrer("trazo_ref%3DABCD2345".replace("%3D", "=")))
        assertNull(ReferralCodes.fromInstallReferrer("utm_source=google-play&utm_medium=organic"))
        assertNull(ReferralCodes.fromInstallReferrer("trazo_ref=nope"))
        assertNull(ReferralCodes.fromInstallReferrer(null))
        assertNull(ReferralCodes.fromInstallReferrer("trazo_ref=" + "A".repeat(600)))
    }

    @Test fun `a typed code wins as manual, the pending one keeps its source`() {
        val pending = PendingReferral("ABCD2345", ReferralSource.LINK)
        assertEquals(pending, referralFor(null, pending))
        assertEquals(pending, referralFor("abcd-2345", pending))
        assertEquals(PendingReferral("WXYZ6789", ReferralSource.MANUAL), referralFor("wxyz6789", pending))
        assertEquals(PendingReferral("WXYZ6789", ReferralSource.MANUAL), referralFor("WXYZ6789", null))
        assertEquals(pending, referralFor("garbage", pending))
        assertNull(referralFor("garbage", null))
    }

    @Test fun `the invitation message carries the link`() {
        assertEquals("Hola https://x/invite/ABCD2345 fin", invitationMessage("Hola {link} fin", "https://x/invite/ABCD2345"))
    }
}

class ReferralRegistrationTest {
    @Test fun `registering sends the pending code and where it came from, then forgets it`() = runTest {
        val store = MemoryReferralStore().apply { save("ABCD2345", ReferralSource.INSTALL_REFERRER) }
        val api = RecordingAuthApi()

        val outcome = AuthRepository(api, MemoryTokenStore(), store).register("a@b.c", "CorrectHorse1Battery", "Ana")

        assertTrue(outcome is Outcome.EmailVerificationRequired)
        assertEquals("ABCD2345", api.registered?.referralCode)
        assertEquals("install_referrer", api.registered?.referralSource)
        assertNull(store.peek())
    }

    @Test fun `a typed code is sent as manual and replaces the pending one`() = runTest {
        val store = MemoryReferralStore().apply { save("ABCD2345", ReferralSource.LINK) }
        val api = RecordingAuthApi()

        AuthRepository(api, MemoryTokenStore(), store).register("a@b.c", "CorrectHorse1Battery", "Ana", referralCode = "wxyz-6789")

        assertEquals("WXYZ6789", api.registered?.referralCode)
        assertEquals("manual", api.registered?.referralSource)
    }

    @Test fun `without any code nothing is sent`() = runTest {
        val api = RecordingAuthApi()
        AuthRepository(api, MemoryTokenStore(), MemoryReferralStore()).register("a@b.c", "CorrectHorse1Battery", "Ana")
        assertNull(api.registered?.referralCode)
        assertNull(api.registered?.referralSource)
    }

    @Test fun `a failed registration keeps the code for the next attempt`() = runTest {
        val store = MemoryReferralStore().apply { save("ABCD2345", ReferralSource.LINK) }
        val outcome = AuthRepository(RecordingAuthApi(fail = true), MemoryTokenStore(), store).register("a@b.c", "CorrectHorse1Battery", "Ana")
        assertTrue(outcome is Outcome.Error)
        assertEquals("ABCD2345", store.peek()?.code)
    }

    @Test fun `google sign-in sends the pending code too`() = runTest {
        val store = MemoryReferralStore().apply { save("ABCD2345", ReferralSource.LINK) }
        val api = RecordingAuthApi()
        AuthRepository(api, MemoryTokenStore(), store).google("id-token")
        assertEquals("ABCD2345", api.googled?.referralCode)
        assertEquals("link", api.googled?.referralSource)
        assertNull(store.peek())
    }

    @Test fun `only the first invitation is kept`() {
        val store = MemoryReferralStore()
        store.save("ABCD2345", ReferralSource.LINK)
        store.save("WXYZ6789", ReferralSource.INSTALL_REFERRER)
        store.save("nope", ReferralSource.LINK)
        assertEquals(PendingReferral("ABCD2345", ReferralSource.LINK), store.peek())
    }

    @Test fun `the requests carry the optional fields only as the server expects`() {
        val json = Gson().toJson(RegisterRequest("a@b.c", "pw", "Ana", referralCode = "ABCD2345", referralSource = "link"))
        assertTrue(json.contains("\"referralCode\":\"ABCD2345\""))
        assertTrue(json.contains("\"referralSource\":\"link\""))
        assertFalse(Gson().toJson(RegisterRequest("a@b.c", "pw", "Ana")).contains("referral"))
    }
}

private class FakeReferralApi : ReferralApi {
    var me: () -> Response<ReferralMe> = {
        Response.success(ReferralMe(true, 30, "ABCD2345", "https://api.trazo.test/invite/ABCD2345", ReferralStats(2, 1, 1, 1, 30, 0, 30)))
    }
    var invitations: () -> Response<ReferralInvitations> = {
        Response.success(ReferralInvitations(listOf(
            ReferralInvitation("i1", "2026-11-10T10:00:00Z", "rewarded", "2026-11-12T10:00:00Z", "monthly", 30, "deferred"),
            ReferralInvitation("i2", "2026-11-11T10:00:00Z", "registered")
        )))
    }
    var rewards: () -> Response<PremiumGrants> = { Response.success(PremiumGrants(listOf(PremiumGrant("g1", "referral_reward", 30, "deferred", "2026-11-12T10:00:00Z", googleExpiryAfter = "2026-12-20T10:00:00Z")))) }
    val shared = mutableListOf<String?>()
    val events = mutableListOf<String>()
    override suspend fun me() = me.invoke()
    override suspend fun invitations(status: String?) = invitations.invoke()
    override suspend fun rewards() = rewards.invoke()
    override suspend fun shared(request: SharedRequest): Response<Unit> { shared += request.channel; return Response.success(Unit) }
    override suspend fun event(request: ClientEventRequest): Response<Unit> { events += request.name; return Response.success(Unit) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReferralScreenModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setMain() { Dispatchers.setMain(dispatcher) }
    @After fun resetMain() { Dispatchers.resetMain() }

    @Test fun `the screen shows the code, the statistics, the invitations and the Premium earned`() = runTest(dispatcher) {
        val vm = ReferralViewModel(ReferralRepository(FakeReferralApi()))
        advanceUntilIdle()

        val s = vm.state.value
        assertFalse(s.loading)
        assertEquals("ABCD2345", s.me?.code)
        assertEquals(30, s.me?.stats?.daysEarned)
        assertEquals(listOf("rewarded", "registered"), s.invitations.map { it.status })
        assertEquals("deferred", s.grants.single().status)
        assertNull(s.error)
        assertNull(s.listError)
    }

    @Test fun `a failure of the main call shows an error that can be retried`() = runTest(dispatcher) {
        val api = FakeReferralApi().apply { me = { networkFailure() } }
        val vm = ReferralViewModel(ReferralRepository(api))
        advanceUntilIdle()
        assertEquals(ErrorKind.Network, vm.state.value.error?.kind)
        assertNull(vm.state.value.me)

        api.me = { Response.success(ReferralMe(true, 30, "ABCD2345", "https://x/invite/ABCD2345", ReferralStats())) }
        vm.load()
        advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertEquals("ABCD2345", vm.state.value.me?.code)
    }

    @Test fun `failing lists do not hide the code`() = runTest(dispatcher) {
        val api = FakeReferralApi().apply { invitations = { httpError(500) }; rewards = { httpError(500) } }
        val vm = ReferralViewModel(ReferralRepository(api))
        advanceUntilIdle()
        assertEquals("ABCD2345", vm.state.value.me?.code)
        assertNotNull(vm.state.value.listError)
        assertTrue(vm.state.value.invitations.isEmpty())
    }

    @Test fun `an empty rewards response does not crash or hide the referral screen`() = runTest(dispatcher) {
        val api = FakeReferralApi().apply { rewards = { Response.success<PremiumGrants>(204, null) } }
        val vm = ReferralViewModel(ReferralRepository(api))
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals("ABCD2345", state.me?.code)
        assertEquals(2, state.invitations.size)
        assertTrue(state.grants.isEmpty())
        assertEquals(ErrorKind.Unknown, state.listError?.kind)
        assertEquals("empty_response", state.listError?.code)
        assertEquals(204, state.listError?.httpStatus)
    }

    @Test fun `no invitations yet is an empty list, not an error`() = runTest(dispatcher) {
        val api = FakeReferralApi().apply {
            me = { Response.success(ReferralMe(true, 30, "ABCD2345", "l", ReferralStats())) }
            invitations = { Response.success(ReferralInvitations()) }
            rewards = { Response.success(PremiumGrants()) }
        }
        val vm = ReferralViewModel(ReferralRepository(api))
        advanceUntilIdle()
        assertTrue(vm.state.value.invitations.isEmpty())
        assertNull(vm.state.value.error)
        assertNull(vm.state.value.listError)
    }

    @Test fun `a programme that is off shows no code`() = runTest(dispatcher) {
        val api = FakeReferralApi().apply { me = { Response.success(ReferralMe(false, 30)) } }
        val vm = ReferralViewModel(ReferralRepository(api))
        advanceUntilIdle()
        assertEquals(false, vm.state.value.me?.enabled)
        assertNull(vm.state.value.me?.code)
    }

    @Test fun `sharing is counted and refreshing reloads everything`() = runTest(dispatcher) {
        val api = FakeReferralApi()
        val vm = ReferralViewModel(ReferralRepository(api))
        advanceUntilIdle()
        vm.shared()
        advanceUntilIdle()
        assertEquals(1, api.shared.size)

        api.me = { Response.success(ReferralMe(true, 30, "ABCD2345", "l", ReferralStats(3, 2, 1, 1, 60, 0, 0))) }
        vm.load(refresh = true)
        advanceUntilIdle()
        assertEquals(60, vm.state.value.me?.stats?.daysEarned)
        assertFalse(vm.state.value.refreshing)
    }

    @Test fun `a failing share counter never breaks the screen`() = runTest(dispatcher) {
        val api = object : ReferralApi by FakeReferralApi() {
            override suspend fun shared(request: SharedRequest): Response<Unit> = networkFailure()
        }
        val vm = ReferralViewModel(ReferralRepository(api))
        advanceUntilIdle()
        vm.shared()
        advanceUntilIdle()
        assertNull(vm.state.value.error)
    }

    @Test fun `milestones go to the server`() = runTest(dispatcher) {
        val api = FakeReferralApi()
        ReferralRepository(api).onboardingCompleted()
        assertEquals(listOf("onboarding_completed"), api.events)
    }
}

class ReferralContractTest {
    @Test fun `the server answers are read, including fields the app does not know`() {
        val me = Gson().fromJson(
            """{"enabled":true,"rewardDays":45,"code":"ABCD2345","link":"https://api.trazo.test/invite/ABCD2345","future":1,
               "stats":{"invited":3,"pending":2,"converted":1,"rewards":1,"daysEarned":45,"daysPending":0,"daysDeferred":45}}""", ReferralMe::class.java)
        assertTrue(me.enabled)
        assertEquals(45, me.rewardDays)
        assertEquals(45, me.stats?.daysEarned)

        val list = Gson().fromJson(
            """{"items":[{"id":"x","registeredAt":"2026-11-10T10:00:00+00:00","status":"rewarded","convertedAt":"2026-11-12T10:00:00+00:00","plan":"annual","rewardDays":45,"rewardState":"active"}]}""",
            ReferralInvitations::class.java)
        assertEquals("annual", list.items.single().plan)
        assertNull(Gson().fromJson("""{"id":"y","registeredAt":"2026-11-10","status":"registered"}""", ReferralInvitation::class.java).rewardDays)
    }

    @Test fun `the subscription status reads the promotional time`() {
        val s = Gson().fromJson("""{"hasAccess":true,"enforced":true,"status":"none","source":"promo","promoExpiresAt":"2026-12-14T12:00:00+00:00"}""",
            com.trazo.android.data.model.SubscriptionStatus::class.java)
        assertTrue(s.hasAccess)
        assertEquals("promo", s.source)
        assertEquals("2026-12-14T12:00:00+00:00", s.promoExpiresAt)
        // An older server does not send it.
        assertNull(Gson().fromJson("""{"hasAccess":false}""", com.trazo.android.data.model.SubscriptionStatus::class.java).promoExpiresAt)
    }

    @Test fun `every call tells the server which version of the app is asking`() {
        val request = okhttp3.Request.Builder().url("https://api.trazo.test/api/v1/referrals/me").build()
        var sent: okhttp3.Request? = null
        val chain = object : okhttp3.Interceptor.Chain {
            override fun request() = request
            override fun proceed(request: okhttp3.Request): okhttp3.Response { sent = request; return okhttp3.Response.Builder().request(request).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("ok").build() }
            override fun connection() = null
            override fun call(): okhttp3.Call = throw UnsupportedOperationException()
            override fun connectTimeoutMillis() = 0
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun readTimeoutMillis() = 0
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun writeTimeoutMillis() = 0
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }
        ContextInterceptor(language = { "es" }, zone = { "Europe/Madrid" }, appVersion = { "0.3.0" }).intercept(chain)
        assertEquals("0.3.0", sent?.header("X-App-Version"))
    }
}

class CampaignAttributionTest {
    @Test fun `the campaign is read from the Play install referrer among the utm parameters`() {
        assertEquals("spring-ads", CampaignCodes.fromInstallReferrer("utm_source=ads&utm_medium=trazo&utm_campaign=spring-ads&trazo_cmp=spring-ads"))
        assertEquals("spring-ads", CampaignCodes.fromInstallReferrer("trazo_cmp=Spring-Ads"))
        assertNull(CampaignCodes.fromInstallReferrer("utm_source=google-play&utm_medium=organic"))
        assertNull(CampaignCodes.fromInstallReferrer("trazo_cmp=-bad-"))
        assertNull(CampaignCodes.fromInstallReferrer("trazo_cmp='; DROP TABLE users;--"))
        assertNull(CampaignCodes.fromInstallReferrer("trazo_cmp=" + "a".repeat(600)))
        assertNull(CampaignCodes.fromInstallReferrer(null))
    }

    @Test fun `the campaign link name has the shape the server accepts`() {
        listOf("abc", "spring-ads", "a1-b2").forEach { assertEquals(it, CampaignCodes.normalize(it)) }
        listOf(null, "", "ab", "has space", "UP_PER", "-start", "end-", "a".repeat(33)).forEach { assertNull(it, CampaignCodes.normalize(it)) }
    }

    @Test fun `registering sends the campaign and forgets it, with or without an invitation`() = runTest {
        val store = MemoryReferralStore().apply { saveCampaign("radio-ad") }
        val api = RecordingAuthApi()
        AuthRepository(api, MemoryTokenStore(), store).register("a@b.c", "CorrectHorse1Battery", "Ana")
        assertEquals("radio-ad", api.registered?.campaignCode)
        assertNull(api.registered?.referralCode)
        assertNull(store.campaign)
    }

    @Test fun `a failed registration keeps the campaign and google sign-in sends it too`() = runTest {
        val store = MemoryReferralStore().apply { saveCampaign("radio-ad") }
        AuthRepository(RecordingAuthApi(fail = true), MemoryTokenStore(), store).register("a@b.c", "CorrectHorse1Battery", "Ana")
        assertEquals("radio-ad", store.campaign)
        val api = RecordingAuthApi()
        AuthRepository(api, MemoryTokenStore(), store).google("id-token")
        assertEquals("radio-ad", api.googled?.campaignCode)
        assertNull(store.campaign)
    }

    @Test fun `only the first campaign is kept`() {
        val store = MemoryReferralStore()
        store.saveCampaign("first-one")
        store.saveCampaign("second-one")
        assertEquals("first-one", store.campaign)
    }
}
