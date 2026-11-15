package com.trazo.android.data

import com.trazo.android.data.api.AnalyticsApi
import com.trazo.android.data.model.PaywallViewRequest
import com.trazo.android.data.repo.AnalyticsRepository
import com.trazo.android.testing.networkFailure
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class AnalyticsRepositoryTest {
    private class RecordingApi(private val fail: Boolean = false) : AnalyticsApi {
        val sent = mutableListOf<String>()
        override suspend fun paywallView(request: PaywallViewRequest): Response<Unit> {
            sent += request.source
            if (fail) networkFailure()
            return Response.success(Unit)
        }
    }

    @Test fun `the place the offer was opened from is sent as is`() = runTest {
        val api = RecordingApi()
        AnalyticsRepository(api).paywallViewed("session")
        assertEquals(listOf("session"), api.sent)
    }

    @Test fun `a failure is swallowed so the offer never breaks`() = runTest {
        val api = RecordingApi(fail = true)
        AnalyticsRepository(api).paywallViewed("account")
        assertEquals(listOf("account"), api.sent)
    }
}
