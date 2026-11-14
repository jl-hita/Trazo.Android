package com.trazo.android.data

import com.google.gson.Gson
import com.trazo.android.auth.AuthApi
import com.trazo.android.data.api.PlansApi
import com.trazo.android.data.net.*
import com.trazo.android.testing.MemoryTokenStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Authentication interceptor, 401 → refresh → retry, and 402 / expired-session events, against a real HTTP server. */
class NetworkModuleTest {
    private lateinit var server: MockWebServer
    private lateinit var tokens: MemoryTokenStore
    private lateinit var events: SessionEvents
    private lateinit var plans: PlansApi

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        tokens = MemoryTokenStore("old-access", "old-refresh")
        events = SessionEvents()
        plans = NetworkModule(server.url("/").toString(), tokens, events, Gson()).create(PlansApi::class.java)
    }

    @After fun tearDown() { server.shutdown() }

    private val weekJson = """{"planId":"p","weekNumber":1,"weekStart":"2026-11-16","weekEnd":"2026-11-22","timeline":"InProgress","mesocycle":null,"sessions":[]}"""

    @Test fun `requests carry the bearer token`() = runBlocking {
        server.enqueue(MockResponse().setBody(weekJson))
        assertTrue(apiCall { plans.week() } is ApiResult.Success)
        assertEquals("Bearer old-access", server.takeRequest().getHeader("Authorization"))
    }

    @Test fun `a 401 refreshes the token once and retries the call`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setBody("""{"accessToken":"new-access","refreshToken":"new-refresh"}"""))
        server.enqueue(MockResponse().setBody(weekJson))
        assertTrue(apiCall { plans.week() } is ApiResult.Success)
        server.takeRequest()
        val refresh = server.takeRequest()
        assertEquals("/api/v1/auth/refresh", refresh.path)
        assertTrue(refresh.body.readUtf8().contains("old-refresh"))
        assertEquals("Bearer new-access", server.takeRequest().getHeader("Authorization"))
        assertEquals("new-access", tokens.access)
        assertEquals("new-refresh", tokens.refresh)
    }

    @Test fun `a rejected refresh token ends the session`() = runTest(UnconfinedTestDispatcher()) {
        val seen = mutableListOf<SessionEvent>()
        events.events.onEach { seen += it }.launchIn(backgroundScope)
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401)) // refresh refused
        val result = apiCall { plans.week() } as ApiResult.Failure
        assertEquals(ErrorKind.Unauthorized, result.error.kind)
        assertNull(tokens.refresh)
        assertEquals(listOf(SessionEvent.SessionExpired), seen)
    }

    @Test fun `being offline during a refresh keeps the session`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START))
        val result = apiCall { plans.week() } as ApiResult.Failure
        assertEquals("old-refresh", tokens.refresh)
        assertTrue(result.error.kind == ErrorKind.Unauthorized || result.error.kind == ErrorKind.Network)
    }

    @Test fun `a 402 raises the subscription event and reaches the caller`() = runTest(UnconfinedTestDispatcher()) {
        val seen = mutableListOf<SessionEvent>()
        events.events.onEach { seen += it }.launchIn(backgroundScope)
        server.enqueue(MockResponse().setResponseCode(402).setBody("""{"error":"subscription_required"}"""))
        val result = apiCall { plans.week() } as ApiResult.Failure
        assertEquals(ErrorKind.SubscriptionRequired, result.error.kind)
        assertEquals(listOf(SessionEvent.SubscriptionRequired), seen)
    }

    @Test fun `authentication endpoints never get the bearer token added`() = runBlocking {
        val auth = NetworkModule(server.url("/").toString(), tokens, events, Gson()).create(AuthApi::class.java)
        server.enqueue(MockResponse().setBody("""{"accessToken":"a","refreshToken":"b"}"""))
        auth.refresh(com.trazo.android.auth.RefreshRequest("r"))
        assertNull(server.takeRequest().getHeader("Authorization"))
    }
}
