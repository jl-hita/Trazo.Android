package com.trazo.android.data.repo

import com.trazo.android.data.api.AccountApi
import com.trazo.android.data.model.AccountInfo
import com.trazo.android.data.model.DeleteAccountRequest
import com.trazo.android.data.model.DeleteAccountResult
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.apiCall

/** Deleting the account: Google Play requires it to be possible from inside the app. */
class AccountRepository(private val api: AccountApi) {
    suspend fun info(): ApiResult<AccountInfo> = apiCall { api.info() }
    suspend fun delete(request: DeleteAccountRequest): ApiResult<DeleteAccountResult> = apiCall { api.delete(request) }
}
