package com.trazo.android.data

import com.google.gson.Gson
import com.trazo.android.data.local.JsonFileStore
import com.trazo.android.data.local.PendingLogQueue
import com.trazo.android.data.local.PlanCache
import com.trazo.android.data.model.NextSessionView
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

/** What to do about the sessions not done: the advice is read from the server and omitting is a deliberate call that refreshes the plan. */
class SessionsNotDoneTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var api: FakePlansApi
    private lateinit var repo: PlanRepository

    @Before fun setUp() {
        api = FakePlansApi()
        repo = PlanRepository(
            api, PlanCache(JsonFileStore(File(tmp.root, "c")), { 0L }, { LocalDate.of(2026, 11, 17) }), PendingLogQueue(JsonFileStore(File(tmp.root, "q")))
        )
    }

    @Test fun `the advice of the server is read and an older server without it still works`() {
        val next = Gson().fromJson(
            """{"planId":"p","schedulingMode":"Sequence","timeline":"InProgress","session":null,"overdue":[],
              "adjustment":{"strategy":"Rebuild","reason":"RepeatedSkips","message":"Has omitido varias.","easeBackSessions":1,
                            "canReschedule":false,"suggestSkip":false,"canSkip":true}}""",
            NextSessionView::class.java
        )
        assertEquals("Rebuild", next.adjustment!!.strategy)
        assertEquals(1, next.adjustment!!.easeBackSessions)
        assertTrue(next.adjustment!!.canSkip)
        val old = Gson().fromJson("""{"planId":"p","schedulingMode":"Calendar","timeline":"InProgress","session":null,"overdue":[]}""", NextSessionView::class.java)
        assertNull(old.adjustment)
    }

    @Test fun `omitting sends the reason, drops the cached week and reports the result`() = runTest {
        repo.currentWeek()
        assertNotNull(repo.week.value)
        val result = repo.skip("s1", "Time") as ApiResult.Success
        assertEquals(listOf("s1" to "Time"), api.skipped.map { it.first to it.second.reason })
        assertEquals("ok", result.value.message)
        assertNull(repo.week.value)
    }

    @Test fun `a refused omission keeps the week and reports the error`() = runTest {
        repo.currentWeek()
        api.skipResponse = { httpError(409, problem(409, "already_logged")) }
        assertTrue(repo.skip("s1", "Other") is ApiResult.Failure)
        assertNotNull(repo.week.value)
    }
}
