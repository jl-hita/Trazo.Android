package com.trazo.android.data

import com.google.gson.Gson
import com.trazo.android.data.local.JsonFileStore
import com.trazo.android.data.local.PendingLogQueue
import com.trazo.android.data.local.PlanCache
import com.trazo.android.data.model.AdaptationHistoryView
import com.trazo.android.data.model.LastPerformanceView
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.PlanRepository
import com.trazo.android.testing.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

/** What the engine adjusted is read from the server with its numbers; a server without it (or an old one) still works. */
class AdaptationHistoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var api: FakePlansApi
    private lateinit var repo: PlanRepository

    @Before fun setUp() {
        api = FakePlansApi()
        repo = PlanRepository(
            api, PlanCache(JsonFileStore(File(tmp.root, "c")), { 0L }, { LocalDate.of(2026, 11, 17) }), PendingLogQueue(JsonFileStore(File(tmp.root, "q")))
        )
    }

    @Test fun `the adjustments are read with their summary, exercises and the session that caused them`() {
        val history = Gson().fromJson(
            """{"reviews":2,"changes":1,"sessionsAdjusted":3,"automatic":true,"entries":[
                {"id":"a1","at":"2026-11-17T10:00:00+00:00","kind":"reduction_sets","summary":"Una serie menos en Press banca en las próximas 3 sesiones.",
                 "explanation":"Fatiga repetida.","exercises":[{"exerciseId":"bench-press","name":"Press banca"}],"sessionsChanged":3,"changed":true,
                 "sourceSession":"Empuje A","sourceDate":"2026-11-17"},
                {"id":"a2","at":"2026-11-16T10:00:00+00:00","kind":"review","summary":"Sesión revisada: no hacía falta cambiar el plan.","explanation":"Todo en orden.","exercises":[],"sessionsChanged":0,"changed":false}]}""",
            AdaptationHistoryView::class.java
        )
        assertEquals(3, history.sessionsAdjusted)
        assertEquals("Press banca", history.entries[0].exercises.single().name)
        assertTrue(history.entries[0].changed)
        assertFalse(history.entries[1].changed)
        assertTrue(history.entries[1].exercises.isEmpty())
    }

    @Test fun `the estimated load says how many sessions it comes from, and an older server without it still parses`() {
        val estimate = Gson().fromJson(
            """{"date":"2026-11-16","sets":[],"suggestedLoadKg":62.5,"suggestionBasis":"estimate","basedOnSessions":3}""", LastPerformanceView::class.java
        )
        assertEquals("estimate", estimate.suggestionBasis)
        assertEquals(3, estimate.basedOnSessions)
        val old = Gson().fromJson("""{"date":"2026-11-16","sets":[],"suggestedLoadKg":60.0,"suggestionBasis":"repeat"}""", LastPerformanceView::class.java)
        assertNull(old.basedOnSessions)
    }

    @Test fun `the repository returns the history and reports a failure`() = runTest {
        api.adaptationsResponse = { retrofit2.Response.success(AdaptationHistoryView(reviews = 1, automatic = true)) }
        assertEquals(1, (repo.adaptations() as ApiResult.Success).value.reviews)
        api.adaptationsResponse = { httpError(404, problem(404, "no_active_plan")) }
        assertTrue(repo.adaptations() is ApiResult.Failure)
    }
}
