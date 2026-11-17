package com.trazo.android.data

import com.google.gson.Gson
import com.trazo.android.data.api.PlansApi
import com.trazo.android.data.local.JsonFileStore
import com.trazo.android.data.local.PendingLogQueue
import com.trazo.android.data.local.PlanCache
import com.trazo.android.data.model.*
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.ContextInterceptor
import com.trazo.android.data.net.NetworkModule
import com.trazo.android.data.net.SessionEvents
import com.trazo.android.data.repo.PlanRepository
import com.trazo.android.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.File
import java.time.LocalDate

/** The app tells the API its language and time zone, and can move a plan that fell behind the calendar to this week. */
class RescheduleAndLanguageTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var server: MockWebServer
    private lateinit var api: FakePlansApi
    private lateinit var repo: PlanRepository

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        api = FakePlansApi()
        repo = PlanRepository(
            api, PlanCache(JsonFileStore(File(tmp.root, "c")), { 0L }, { LocalDate.of(2026, 11, 17) }), PendingLogQueue(JsonFileStore(File(tmp.root, "q")))
        )
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun `every request carries the language of the app and the time zone of the device`() = runBlocking {
        val tokens = MemoryTokenStore("a", "r")
        val plans = NetworkModule(server.url("/").toString(), tokens, SessionEvents(), Gson()).create(PlansApi::class.java)
        server.enqueue(MockResponse().setBody("""{"planId":"p","weekNumber":1,"weekStart":null,"weekEnd":null,"timeline":"InProgress","mesocycle":null,"sessions":[]}"""))
        plans.week()
        val request = server.takeRequest()
        assertTrue(request.getHeader("Accept-Language") in setOf("es", "en"))
        assertEquals(java.time.ZoneId.systemDefault().id, request.getHeader("X-Time-Zone"))
        assertEquals("Bearer a", request.getHeader("Authorization"))
    }

    @Test fun `the language is English only when the device is English`() {
        fun header(language: String): String? {
            val chain = FakeChain(okhttp3.Request.Builder().url("http://localhost/").build())
            ContextInterceptor(language = { language }, zone = { "Europe/Madrid" }).intercept(chain)
            return chain.proceeded!!.header("Accept-Language")
        }
        assertEquals("en", header("en"))
        assertEquals("es", header("es"))
    }

    @Test fun `the offer to move the program and the lighter session are read from the next session`() {
        val next = Gson().fromJson(
            """{"planId":"p","schedulingMode":"Calendar","timeline":"InProgress","session":null,"availabilityLocked":false,"overdue":[],
              "reentry":{"daysSinceLastWorkout":25,"message":"Llevas 25 días sin entrenar.","applied":true},
              "reschedule":{"weeksBehind":2}}""",
            NextSessionView::class.java
        )
        assertEquals(2, next.reschedule!!.weeksBehind)
        assertTrue(next.reentry!!.applied)
        // An older server sends neither: both are optional.
        val old = Gson().fromJson("""{"planId":"p","schedulingMode":"Calendar","timeline":"InProgress","session":null,"overdue":[]}""", NextSessionView::class.java)
        assertNull(old.reschedule)
    }

    @Test fun `moving the plan asks the server and drops the cached week so the next read is fresh`() = runTest {
        repo.currentWeek()
        assertNotNull(repo.week.value)
        val result = repo.reschedule() as ApiResult.Success
        assertEquals(1, api.rescheduleCalls)
        assertEquals(8, result.value.sessionsMoved)
        assertNull(repo.week.value)
    }

    @Test fun `a rejected move keeps the week and reports the error`() = runTest {
        repo.currentWeek()
        api.rescheduleResponse = { httpError(400, problem(400, "not_allowed")) }
        assertTrue(repo.reschedule() is ApiResult.Failure)
        assertNotNull(repo.week.value)
    }

    private class FakeChain(private val original: okhttp3.Request) : okhttp3.Interceptor.Chain {
        var proceeded: okhttp3.Request? = null
        override fun request() = original
        override fun proceed(request: okhttp3.Request): okhttp3.Response {
            proceeded = request
            return okhttp3.Response.Builder().request(request).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("OK").build()
        }
        override fun connection() = null
        override fun call() = throw UnsupportedOperationException()
        override fun connectTimeoutMillis() = 0
        override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        override fun readTimeoutMillis() = 0
        override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        override fun writeTimeoutMillis() = 0
        override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
    }
}
