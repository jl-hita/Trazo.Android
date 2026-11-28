package com.trazo.android.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.android.R
import com.trazo.android.data.api.FeedbackApi
import com.trazo.android.data.model.FeedbackRequest
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.apiCall
import com.trazo.android.diagnostics.launchGuarded
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FeedbackState(
    val category: String = "bug",
    val message: String = "",
    val sending: Boolean = false,
    val sent: Boolean = false,
    val error: ApiError? = null
) {
    val canSend: Boolean get() = message.isNotBlank() && !sending && !sent
}

/**
 * The comment being written. A failed send keeps the text and the request id, so trying again cannot store it twice.
 * [screen] is where the person came from and [device] the phone model and Android version.
 */
class FeedbackViewModel(
    private val api: FeedbackApi,
    private val screen: String?,
    private val device: String,
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() }
) : ViewModel() {
    private val _state = MutableStateFlow(FeedbackState())
    val state: StateFlow<FeedbackState> = _state.asStateFlow()
    private var requestId = newId()

    fun category(value: String) = change { it.copy(category = value) }

    fun message(value: String) = change { it.copy(message = value.take(MAX_LENGTH)) }

    /** A comment that changed after a failed send is another comment: it gets its own id. */
    private fun change(transform: (FeedbackState) -> FeedbackState) {
        val before = _state.value
        if (before.sending || before.sent) return
        val after = transform(before)
        if (after == before) return
        if (before.error != null) requestId = newId()
        _state.value = after.copy(error = null)
    }

    fun send() {
        val current = _state.value
        if (!current.canSend) return
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launchGuarded(R.string.feature_account, onFailure = { _state.update { it.copy(sending = false) } }) {
            val result = apiCall { api.send(FeedbackRequest(current.category, current.message.trim(), screen, device, requestId)) }
            _state.update {
                when (result) {
                    is ApiResult.Success -> it.copy(sending = false, sent = true)
                    is ApiResult.Failure -> it.copy(sending = false, error = result.error)
                }
            }
        }
    }

    companion object {
        const val MAX_LENGTH = 2000
    }
}
