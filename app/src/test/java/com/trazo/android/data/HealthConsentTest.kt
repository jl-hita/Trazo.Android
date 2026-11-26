package com.trazo.android.data

import com.trazo.android.data.api.AccountApi
import com.trazo.android.data.api.LanguageRequest
import com.trazo.android.data.local.PendingLog
import com.trazo.android.data.model.*
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.HealthConsentRepository
import com.trazo.android.data.repo.HealthConsentStore
import com.trazo.android.testing.networkFailure
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

/** The consent for health data: given on the phone, recorded by the server, withdrawn only when the server says so. */
class HealthConsentTest {
    private class MemoryStore : HealthConsentStore {
        var granted = false
        var pending = false
        override fun granted() = granted
        override fun pending() = pending
        override fun save(granted: Boolean, pending: Boolean) { this.granted = granted; this.pending = pending }
    }

    private class FakeApi : AccountApi {
        var online = true
        var serverConsent = false
        var duringInfo: () -> Unit = {}
        val sent = mutableListOf<Boolean>()
        override suspend fun language(request: LanguageRequest): Response<Unit> = Response.success(Unit)
        override suspend fun info(): Response<AccountInfo> {
            if (!online) networkFailure()
            val answer = AccountInfo(canDelete = true, deletion = DeletionRequirements(), healthConsent = serverConsent)
            duringInfo()
            return Response.success(answer)
        }
        override suspend fun healthConsent(request: HealthConsentRequest): Response<Unit> {
            if (!online) networkFailure()
            sent += request.consent
            serverConsent = request.consent
            return Response.success(Unit)
        }
        override suspend fun delete(request: DeleteAccountRequest) = error("not used here")
    }

    private val store = MemoryStore()
    private val api = FakeApi()
    private val steps = mutableListOf<String>()
    private val repo = HealthConsentRepository(api, store, beforeWithdraw = { steps += "before" }, afterWithdraw = { steps += "after" })

    @Test fun `it counts at once and reaches the server when there is a connection`() = runTest {
        assertFalse(repo.granted())
        api.online = false
        repo.grant(); repo.sync()
        assertTrue(repo.granted())
        assertTrue(store.pending)
        assertTrue(api.sent.isEmpty())

        api.online = true
        repo.refresh() // what was given offline is sent, not overwritten by the server's old answer
        assertEquals(listOf(true), api.sent)
        assertTrue(repo.granted())
        assertFalse(store.pending)
    }

    @Test fun `what the server holds is adopted`() = runTest {
        api.serverConsent = true
        repo.refresh()
        assertTrue(repo.granted())
        api.serverConsent = false // withdrawn on another phone
        repo.refresh()
        assertFalse(repo.granted())
    }

    @Test fun `an answer older than a change made here is ignored`() = runTest {
        api.duringInfo = { repo.grant() }
        repo.refresh()
        assertTrue(repo.granted())
    }

    @Test fun `withdrawing needs the server and cleans the phone around it`() = runTest {
        repo.grant(); repo.sync()
        api.online = false
        assertTrue(repo.withdraw() is ApiResult.Failure)
        assertTrue(repo.granted())
        assertEquals(listOf("before"), steps)

        api.online = true
        assertTrue(repo.withdraw() is ApiResult.Success)
        assertFalse(repo.granted())
        assertEquals(listOf("before", "before", "after"), steps)
    }

    @Test fun `signing out forgets it`() {
        repo.grant()
        repo.clear()
        assertFalse(repo.granted())
        assertFalse(store.pending)
    }

    @Test fun `a queued workout loses its health marks and keeps the rest`() {
        val log = PendingLog(
            "s1",
            LogSessionRequest(
                outcome = "Partial", nonCompletionReason = "Illness", painReported = true, notes = "mine",
                sets = listOf(LogSetRequest("bench", 1, 60.0, 8, 2.0, true, painReported = true), LogSetRequest("bench", 2, null, null, null, false, "illness")),
                exerciseFeedback = mapOf(
                    "bench" to ExerciseFeedbackRequest(stimulus = "adequate", discomfort = "strong"),
                    "squat" to ExerciseFeedbackRequest(discomfort = "mild")
                ),
                clientRequestId = "id-1"
            ),
            createdAtMillis = 5, failureKind = "Validation"
        )
        val clean = log.withoutHealthData()
        assertTrue(clean.sameAs(log))
        assertTrue(clean.isRejected)
        with(clean.request) {
            assertEquals("Other", nonCompletionReason)
            assertFalse(painReported)
            assertEquals("mine", notes)
            assertEquals("id-1", clientRequestId)
            assertFalse(sets[0].painReported)
            assertEquals(60.0, sets[0].weightKg!!, 0.0)
            assertEquals("other", sets[1].skippedReason)
            assertEquals(mapOf("bench" to ExerciseFeedbackRequest(stimulus = "adequate")), exerciseFeedback)
        }
    }
}
