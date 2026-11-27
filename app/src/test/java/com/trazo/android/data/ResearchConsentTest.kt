package com.trazo.android.data

import com.trazo.android.data.api.AccountApi
import com.trazo.android.data.api.LanguageRequest
import com.trazo.android.data.model.*
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.ResearchConsentRepository
import com.trazo.android.testing.networkFailure
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

/** The research consent lives on the server: off until turned on, and a change counts only once the server has it. */
class ResearchConsentTest {
    private class FakeApi : AccountApi {
        var online = true
        var consent = false
        override suspend fun info(): Response<AccountInfo> {
            if (!online) networkFailure()
            return Response.success(AccountInfo(canDelete = true, deletion = DeletionRequirements(), researchConsent = consent))
        }
        override suspend fun researchConsent(request: ResearchConsentRequest): Response<Unit> {
            if (!online) networkFailure()
            consent = request.consent
            return Response.success(Unit)
        }
        override suspend fun language(request: LanguageRequest): Response<Unit> = error("not used here")
        override suspend fun healthConsent(request: HealthConsentRequest): Response<Unit> = error("not used here")
        override suspend fun delete(request: DeleteAccountRequest) = error("not used here")
    }

    private val api = FakeApi()
    private val repo = ResearchConsentRepository(api)

    private suspend fun read() = (repo.read() as ApiResult.Success).value

    @Test fun `it is off until it is turned on, and can be turned off again`() = runTest {
        assertFalse(read())
        assertTrue(repo.set(true) is ApiResult.Success)
        assertTrue(read())
        repo.set(false)
        assertFalse(read())
    }

    @Test fun `without a connection nothing changes`() = runTest {
        api.online = false
        assertTrue(repo.read() is ApiResult.Failure)
        assertTrue(repo.set(true) is ApiResult.Failure)
        assertFalse(api.consent)
    }
}
