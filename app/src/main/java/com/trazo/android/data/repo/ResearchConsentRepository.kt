package com.trazo.android.data.repo

import com.trazo.android.data.api.AccountApi
import com.trazo.android.data.model.ResearchConsentRequest
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.apiCall

/**
 * The optional consent to use the training data for research. Only the server keeps it: it is off until the person turns it on, and
 * a change does not count until the server has it.
 */
class ResearchConsentRepository(private val api: AccountApi) {
    suspend fun read(): ApiResult<Boolean> = when (val result = apiCall { api.info() }) {
        is ApiResult.Success -> ApiResult.Success(result.value.researchConsent)
        is ApiResult.Failure -> result
    }

    /**
     * Why-do-you-archive is asked only with the research consent in force, read from the server each time. Null when there is none or
     * it cannot be known: the plan is then archived without a question, because archiving never depends on this consent.
     */
    suspend fun archiveQuestion(): ArchiveQuestion? {
        val account = (apiCall { api.info() } as? ApiResult.Success)?.value ?: return null
        if (!account.researchConsent) return null
        return ArchiveQuestion(ArchiveReason.entries.filter { it != ArchiveReason.Discomfort || account.healthConsent })
    }

    suspend fun set(consent: Boolean): ApiResult<Unit> = apiCall { api.researchConsent(ResearchConsentRequest(consent)) }
}
