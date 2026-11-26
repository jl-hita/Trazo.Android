package com.trazo.android.data

import com.trazo.android.data.local.*
import com.trazo.android.data.model.LogResult
import com.trazo.android.data.model.LogSessionRequest
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.data.repo.LogOutcome
import com.trazo.android.data.repo.PlanRepository
import com.trazo.android.data.sync.SyncScheduler
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

private class CountingScheduler : SyncScheduler {
    var scheduled = 0
    var cancelled = 0
    override fun schedule() { scheduled++ }
    override fun cancel() { cancelled++ }
}

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

    @Test fun `no review request yet is a success with nothing`() = runTest {
        assertNull((repo.reviewRequest() as ApiResult.Success).value)
    }

    @Test fun `a blank note is sent as no note and the request comes back pending`() = runTest {
        val result = repo.requestReview("   ") as ApiResult.Success
        assertNull(api.reviewedWith!!.note)
        assertEquals("pending", result.value.status)
        repo.requestReview("  me duele el hombro ")
        assertEquals("me duele el hombro", api.reviewedWith!!.note)
    }

    @Test fun `a message to the team is trimmed and comes back in the conversation`() = runTest {
        val result = repo.sendReviewMessage("  hola  ", "message-1") as ApiResult.Success
        assertEquals("hola", api.sentMessage!!.body)
        assertEquals("hola", result.value.messages!!.single().body)
    }

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

    @Test fun `payment required keeps the local copy, the plan is still readable`() = runTest {
        repo.currentWeek()
        api.weekResponse = { httpError(402, """{"title":"premium_required"}""") }
        val result = repo.currentWeek() as ApiResult.Failure
        assertEquals(ErrorKind.SubscriptionRequired, result.error.kind)
        assertNotNull(cache.week())
        assertNotNull(repo.week.value)
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
        val scheduler = CountingScheduler()
        val restarted = PlanRepository(api, cache, PendingLogQueue(JsonFileStore(File(tmp.root, "queue"))), sync = scheduler)
        restarted.resumePendingSync()
        assertEquals(1, restarted.pendingCount.value)
        assertEquals(1, scheduler.scheduled)
    }

    @Test fun `queuing a log schedules the background sync and signing out cancels it`() = runTest {
        val scheduler = CountingScheduler()
        val synced = PlanRepository(api, cache, queue, sync = scheduler)
        api.logResponse = { _, _ -> networkFailure() }
        synced.submitLog("s1", logRequest(), now)
        assertEquals(1, scheduler.scheduled)
        synced.clearLocal()
        assertEquals(1, scheduler.cancelled)
    }

    @Test fun `nothing is scheduled when the queue is empty`() = runTest {
        val scheduler = CountingScheduler()
        PlanRepository(api, cache, queue, sync = scheduler).resumePendingSync()
        assertEquals(0, scheduler.scheduled)
    }

    @Test fun `server errors are queued, validation errors are returned`() = runTest {
        api.logResponse = { _, _ -> httpError(503) }
        assertEquals(LogOutcome.Queued, repo.submitLog("s1", logRequest(), now))
        api.logResponse = { _, _ -> httpError(400, problem(400, "invalid_log")) }
        val rejected = repo.submitLog("s2", logRequest(), now)
        assertTrue(rejected is LogOutcome.Rejected)
        assertEquals(1, queue.all().size) // s2 was not queued
    }

    @Test fun `a session already logged is delivered online, but a queued one is kept for the user`() = runTest {
        api.logResponse = { _, _ -> httpError(409, problem(409, "session_already_logged")) }
        assertEquals(LogOutcome.AlreadyLogged, repo.submitLog("s1", logRequest()))
        queue.add(PendingLog("s1", logRequest(), now))
        assertEquals(1, repo.flushPending())
        assertEquals("session_already_logged", queue.all().single().failureCode)
    }

    @Test fun `a rejected queued log does not block the rest`() = runTest {
        queue.add(PendingLog("bad", logRequest(), 1))
        queue.add(PendingLog("good", logRequest(), 2))
        api.logResponse = { id, _ -> if (id == "bad") httpError(404, problem(404, "session_not_found")) else ok() }
        assertEquals(2, repo.flushPending())
        assertEquals(listOf("bad"), queue.all().map { it.sessionId })
        assertEquals(0, repo.pendingCount.value)
        assertEquals(listOf("bad"), repo.rejected.value.map { it.sessionId })
    }

    @Test fun `an expired session keeps the unsent workouts and says nothing against them`() = runTest {
        api.logResponse = { _, _ -> httpError(401) }
        assertEquals(LogOutcome.Queued, repo.submitLog("s1", logRequest(), now))
        queue.add(PendingLog("s2", logRequest(), now + 1))
        val calls = api.logCalls.size
        assertEquals(0, repo.flushPending())
        assertEquals(calls + 1, api.logCalls.size) // without a session the rest are not even tried
        repo.forgetCachedPlan()
        assertNull(cache.week())
        assertEquals(listOf("s1", "s2"), queue.all().map { it.sessionId })
        assertTrue(queue.all().none { it.isRejected })
    }

    @Test fun `a queued log refused for lack of Premium shows why and can be retried`() = runTest {
        queue.add(PendingLog("s1", logRequest(), now))
        api.logResponse = { _, _ -> httpError(402, problem(402, "premium_required")) }
        assertEquals(1, repo.flushPending())
        assertEquals(ErrorKind.SubscriptionRequired, repo.rejected.value.single().failure!!.kind)
        api.logResponse = { _, _ -> ok() }
        assertEquals(1, repo.retryRejected(repo.rejected.value.single()))
        assertTrue(queue.all().isEmpty())
    }

    // ---- outbox: saved before it is sent

    @Test fun `the log is already saved while it is being sent`() = runTest {
        var savedDuringSend = 0
        api.logResponse = { _, _ -> savedDuringSend = queue.all().size; ok() }
        repo.submitLog("s1", logRequest(), now)
        assertEquals(1, savedDuringSend)
        assertEquals(0, queue.all().size)
    }

    @Test fun `a send that is cancelled keeps the log and schedules it`() = runTest {
        val scheduler = CountingScheduler()
        val synced = PlanRepository(api, cache, queue, sync = scheduler)
        api.logResponse = { _, _ -> throw kotlinx.coroutines.CancellationException("screen closed") }
        try { synced.submitLog("s1", logRequest(), now); fail() } catch (_: kotlinx.coroutines.CancellationException) { }
        assertEquals("s1", queue.all().single().sessionId)
        assertEquals(1, synced.pendingCount.value)
        assertEquals(1, scheduler.scheduled)
    }

    // ---- rejected logs

    @Test fun `a rejected log keeps its reason, restores the draft once and is not sent again on its own`() = runTest {
        val restored = mutableListOf<String>()
        val r = PlanRepository(api, cache, queue, restoreDraft = { restored += it.sessionId })
        queue.add(PendingLog("s1", logRequest(), now))
        api.logResponse = { _, _ -> httpError(400, problem(400, "invalid_log")) }
        r.flushPending()
        r.flushPending()
        assertEquals(1, api.logCalls.size)
        assertEquals(listOf("s1"), restored)
        assertEquals(ErrorKind.Validation, queue.all().single().failure!!.kind)
        val restarted = PlanRepository(api, cache, PendingLogQueue(JsonFileStore(File(tmp.root, "queue"))))
        restarted.resumePendingSync()
        assertEquals(0, restarted.pendingCount.value)
        assertEquals(1, restarted.rejected.value.size)
    }

    @Test fun `a rejected log can be sent again, unchanged`() = runTest {
        queue.add(PendingLog("s1", logRequest(), now))
        api.logResponse = { _, _ -> httpError(404, problem(404, "session_not_found")) }
        repo.flushPending()
        api.logResponse = { _, _ -> ok() }
        assertEquals(1, repo.retryRejected(repo.rejected.value.single()))
        assertEquals(0, queue.all().size)
        assertTrue(repo.rejected.value.isEmpty())
        assertEquals(api.logCalls.first().second, api.logCalls.last().second)
    }

    @Test fun `a rejected log is only deleted when the user says so`() = runTest {
        val deleted = mutableListOf<String>()
        val r = PlanRepository(api, cache, queue, deleteDraft = { deleted += it })
        queue.add(PendingLog("s1", logRequest(), now))
        api.logResponse = { _, _ -> httpError(400, problem(400, "invalid_log")) }
        r.flushPending()
        r.discardRejected(r.rejected.value.single())
        assertEquals(0, queue.all().size)
        assertEquals(listOf("s1"), deleted)
        assertTrue(r.rejected.value.isEmpty())
    }

    @Test fun `removing an older log leaves the newer one of the same session`() {
        val old = PendingLog("s1", logRequest(), 1)
        queue.add(old)
        queue.add(PendingLog("s1", logRequest(), 2))
        queue.remove(old)
        queue.replace(old.waiting())
        assertEquals(2L, queue.all().single().createdAtMillis)
    }

    @Test fun `clearLocal removes cache, queue and drafts`() = runTest {
        var draftsCleared = false
        val r = PlanRepository(api, cache, queue, clearDrafts = { draftsCleared = true })
        r.currentWeek()
        queue.add(PendingLog("s1", logRequest(), now))
        r.clearLocal()
        assertNull(cache.week()); assertNull(cache.plan()); assertEquals(0, queue.all().size); assertTrue(draftsCleared)
        assertNull(r.week.value)
    }

    // ---- 409 plan_modified_concurrently: the server stored nothing; the workout must stay queued and be sent again, unchanged

    private val concurrent: (String, LogSessionRequest) -> Response<LogResult> = { _, _ -> httpError(409, problem(409, "plan_modified_concurrently")) }

    @Test fun `a concurrency conflict keeps the queued entry complete, with its request id`() = runTest {
        queue.add(PendingLog("s1", logRequest(), now))
        val before = queue.all().single()
        api.logResponse = concurrent
        assertEquals(0, repo.flushPending())
        val kept = queue.all().single()
        assertEquals(before.sessionId, kept.sessionId)
        assertEquals(before.createdAtMillis, kept.createdAtMillis)
        assertEquals(before.request, kept.request)
        assertEquals(1, repo.pendingCount.value)
    }

    @Test fun `a concurrency conflict stops the pass so later entries wait and order is kept`() = runTest {
        queue.add(PendingLog("a", logRequest(), 1))
        queue.add(PendingLog("b", logRequest(), 2))
        api.logResponse = concurrent
        assertEquals(0, repo.flushPending())
        assertEquals(listOf("a"), api.logCalls.map { it.first }) // b was not even tried
        assertEquals(listOf("a", "b"), queue.all().map { it.sessionId })
    }

    @Test fun `a later pass completes what the conflict left, with the same request id`() = runTest {
        queue.add(PendingLog("a", logRequest(), 1))
        queue.add(PendingLog("b", logRequest(), 2))
        api.logResponse = concurrent
        repo.flushPending()
        api.logResponse = { _, _ -> ok() }
        assertEquals(2, repo.flushPending())
        assertEquals(0, queue.all().size)
        val sentFirst = api.logCalls.first { it.first == "a" }.second.clientRequestId
        assertNotNull(sentFirst)
        assertEquals(sentFirst, api.logCalls.last { it.first == "a" }.second.clientRequestId)
    }

    @Test fun `a log that hits a concurrency conflict online is queued, not rejected`() = runTest {
        val scheduler = CountingScheduler()
        val synced = PlanRepository(api, cache, queue, sync = scheduler)
        api.logResponse = concurrent
        assertEquals(LogOutcome.Queued, synced.submitLog("s1", logRequest(), now))
        assertEquals(1, queue.all().size)
        assertNotNull(queue.all().single().request.clientRequestId)
        assertEquals(1, scheduler.scheduled)
    }

    @Test fun `other 409 answers keep their behaviour`() = runTest {
        api.logResponse = { _, _ -> httpError(409, problem(409, "active_plan_exists")) }
        assertTrue(repo.submitLog("s1", logRequest(), now) is LogOutcome.Rejected) // not queued online
        assertEquals(0, queue.all().size)
        queue.add(PendingLog("s2", logRequest(), now))
        assertEquals(1, repo.flushPending()) // no longer waiting, but kept
        assertTrue(queue.all().single().isRejected)
    }

    @Test fun `a conflict entry survives a restart and is delivered afterwards`() = runTest {
        api.logResponse = concurrent
        repo.submitLog("s1", logRequest(), now)
        val scheduler = CountingScheduler()
        val restarted = PlanRepository(api, cache, PendingLogQueue(JsonFileStore(File(tmp.root, "queue"))), sync = scheduler)
        restarted.resumePendingSync()
        assertEquals(1, restarted.pendingCount.value)
        assertEquals(1, scheduler.scheduled)
        api.logResponse = { _, _ -> ok() }
        assertEquals(1, restarted.flushPending())
        assertEquals(0, restarted.pendingCount.value)
    }

    @Test fun `repeating a log the server already accepted does not duplicate it`() = runTest {
        api.logResponse = concurrent
        repo.submitLog("s1", logRequest(), now)
        val key = queue.all().single().request.clientRequestId
        // The first attempt had in fact been stored (the answer was lost); the repeat finds the session resolved.
        api.logResponse = { _, _ -> httpError(409, problem(409, "session_already_logged")) }
        assertEquals(1, repo.flushPending())
        assertTrue(queue.all().single().isRejected)
        // Queuing the same workout twice before delivery keeps one entry, and both sends carry one request id.
        api.logResponse = concurrent
        repo.submitLog("s1", logRequest(), now)
        repo.submitLog("s1", logRequest(), now)
        assertEquals(1, queue.all().size)
        assertEquals(key, queue.all().single().request.clientRequestId)
    }
}
