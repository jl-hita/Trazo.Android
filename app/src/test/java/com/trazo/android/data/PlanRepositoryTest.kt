package com.trazo.android.data

import com.trazo.android.data.local.*
import com.trazo.android.data.model.LogResult
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.data.repo.LogOutcome
import com.trazo.android.data.repo.PlanRepository
import com.trazo.android.testing.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.File
import java.time.LocalDate

class PlanRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var api: FakePlansApi
    private lateinit var cache: PlanCache
    private lateinit var queue: PendingLogQueue
    private var now = 1_000_000L
    private lateinit var repo: PlanRepository

    @Before fun setUp() {
        api = FakePlansApi()
        cache = PlanCache(JsonFileStore(File(tmp.root, "cache")), { now }, { LocalDate.of(2026, 11, 17) })
        queue = PendingLogQueue(JsonFileStore(File(tmp.root, "queue")))
        repo = PlanRepository(api, cache, queue)
    }

    private fun ok(): Response<LogResult> = Response.success(LogResult(sampleSession(), sampleReport()))

    @Test fun `week is fetched, cached and shared`() = runTest {
        val result = repo.currentWeek() as ApiResult.Success
        assertFalse(result.value.fromCache)
        assertEquals("s1", repo.session("s1")!!.id)
        assertNotNull(cache.week())
    }

    @Test fun `offline falls back to the short-lived copy`() = runTest {
        repo.currentWeek()
        api.weekResponse = { networkFailure() }
        val fresh = PlanRepository(api, cache, queue) // new process: nothing in memory
        val result = fresh.currentWeek() as ApiResult.Success
        assertTrue(result.value.fromCache)
    }

    @Test fun `the cache expires after its ttl and when the week is over`() = runTest {
        repo.currentWeek()
        now += PlanCache.DEFAULT_TTL_MILLIS + 1
        assertNull(cache.week())
        cache.saveWeek(sampleWeek(end = "2026-11-16")) // week ended before "today" (2026-11-17)
        assertNull(cache.week())
    }

    @Test fun `no cache and no network is an error, not stale data`() = runTest {
        api.weekResponse = { networkFailure() }
        val result = repo.currentWeek() as ApiResult.Failure
        assertEquals(ErrorKind.Network, result.error.kind)
    }

    @Test fun `payment required wipes the local copy`() = runTest {
        repo.currentWeek()
        api.weekResponse = { httpError(402, """{"error":"subscription_required"}""") }
        val result = repo.currentWeek() as ApiResult.Failure
        assertEquals(ErrorKind.SubscriptionRequired, result.error.kind)
        assertNull(cache.week())
        assertNull(repo.week.value)
    }

    @Test fun `no active plan is reported with its code and clears the cache`() = runTest {
        repo.currentWeek()
        api.weekResponse = { httpError(404, problem(404, "no_active_plan")) }
        val result = repo.currentWeek() as ApiResult.Failure
        assertEquals(PlanRepository.NO_ACTIVE_PLAN, result.error.code)
        assertNull(cache.week())
    }

    @Test fun `a log sent online is not queued`() = runTest {
        repo.currentWeek()
        val outcome = repo.submitLog("s1", logRequest())
        assertTrue(outcome is LogOutcome.Sent)
        assertEquals(0, queue.all().size)
    }

    @Test fun `offline logs are queued and delivered later without loss`() = runTest {
        api.logResponse = { _, _ -> networkFailure() }
        assertEquals(LogOutcome.Queued, repo.submitLog("s1", logRequest(), now))
        assertEquals(1, repo.pendingCount.value)
        assertEquals(0, repo.flushPending()) // still offline
        assertEquals(1, queue.all().size)

        api.logResponse = { _, _ -> ok() }
        assertEquals(1, repo.flushPending())
        assertEquals(0, repo.pendingCount.value)
        assertEquals("s1", api.logCalls.last().first)
    }

    @Test fun `queued logs survive a restart`() = runTest {
        api.logResponse = { _, _ -> networkFailure() }
        repo.submitLog("s1", logRequest(), now)
        val restarted = PlanRepository(api, cache, PendingLogQueue(JsonFileStore(File(tmp.root, "queue"))))
        assertEquals(1, restarted.pendingCount.value)
    }

    @Test fun `server errors are queued, validation errors are returned`() = runTest {
        api.logResponse = { _, _ -> httpError(503) }
        assertEquals(LogOutcome.Queued, repo.submitLog("s1", logRequest(), now))
        api.logResponse = { _, _ -> httpError(400, problem(400, "invalid_log")) }
        val rejected = repo.submitLog("s2", logRequest(), now)
        assertTrue(rejected is LogOutcome.Rejected)
        assertEquals(1, queue.all().size) // s2 was not queued
    }

    @Test fun `a session logged twice is treated as delivered`() = runTest {
        api.logResponse = { _, _ -> httpError(409, problem(409, "session_already_logged")) }
        assertEquals(LogOutcome.AlreadyLogged, repo.submitLog("s1", logRequest()))
        queue.add(PendingLog("s1", logRequest(), now))
        assertEquals(1, repo.flushPending())
        assertEquals(0, queue.all().size)
    }

    @Test fun `a rejected queued log does not block the rest`() = runTest {
        queue.add(PendingLog("bad", logRequest(), 1))
        queue.add(PendingLog("good", logRequest(), 2))
        api.logResponse = { id, _ -> if (id == "bad") httpError(404, problem(404, "session_not_found")) else ok() }
        assertEquals(2, repo.flushPending())
        assertEquals(0, queue.all().size)
    }

    @Test fun `clearLocal removes cache, queue and drafts`() = runTest {
        var draftsCleared = false
        val r = PlanRepository(api, cache, queue) { draftsCleared = true }
        r.currentWeek()
        queue.add(PendingLog("s1", logRequest(), now))
        r.clearLocal()
        assertNull(cache.week()); assertNull(cache.plan()); assertEquals(0, queue.all().size); assertTrue(draftsCleared)
        assertNull(r.week.value)
    }
}
