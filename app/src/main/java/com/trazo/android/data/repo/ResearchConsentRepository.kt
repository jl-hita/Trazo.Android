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

    suspend fun set(consent: Boolean): ApiResult<Unit> = apiCall { api.researchConsent(ResearchConsentRequest(consent)) }
}
