package com.trazo.android.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.android.R
import com.trazo.android.data.model.AccountInfo
import com.trazo.android.data.model.DeleteAccountRequest
import com.trazo.android.data.model.DeleteAccountResult
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.AccountRepository
import com.trazo.android.diagnostics.launchGuarded
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DeleteAccountState(
    val loading: Boolean = true,
    val info: AccountInfo? = null,
    val loadError: ApiError? = null,
    val deleting: Boolean = false,
    /** The password, the Google account or the two-factor code was not accepted. */
    val rejected: Boolean = false,
    val deleteError: ApiError? = null,
    /** Set once the account no longer exists. */
    val done: DeleteAccountResult? = null
)

class DeleteAccountViewModel(private val repository: AccountRepository) : ViewModel() {
    private val _state = MutableStateFlow(DeleteAccountState())
    val state: StateFlow<DeleteAccountState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = DeleteAccountState()
        viewModelScope.launchGuarded(R.string.feature_account, onFailure = { _state.update { it.copy(loading = false) } }) {
            _state.value = when (val result = repository.info()) {
                is ApiResult.Success -> DeleteAccountState(loading = false, info = result.value)
                is ApiResult.Failure -> DeleteAccountState(loading = false, loadError = result.error)
            }
        }
    }

    fun delete(password: String? = null, idToken: String? = null, code: String? = null) {
        if (_state.value.deleting || _state.value.done != null) return
        _state.update { it.copy(deleting = true, rejected = false, deleteError = null) }
        val request = DeleteAccountRequest(password?.takeIf { it.isNotEmpty() }, idToken, code?.trim()?.takeIf { it.isNotEmpty() })
        viewModelScope.launchGuarded(R.string.feature_account, onFailure = { _state.update { it.copy(deleting = false) } }) {
            when (val result = repository.delete(request)) {
                is ApiResult.Success -> _state.update { it.copy(deleting = false, done = result.value) }
                is ApiResult.Failure -> {
                    val rejected = result.error.httpStatus == 403
                    _state.update { it.copy(deleting = false, rejected = rejected, deleteError = result.error.takeUnless { rejected }) }
                }
            }
        }
    }
}
