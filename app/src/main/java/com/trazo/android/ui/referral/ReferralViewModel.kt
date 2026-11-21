package com.trazo.android.ui.referral

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.android.R
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.referral.PremiumGrant
import com.trazo.android.data.referral.ReferralInvitation
import com.trazo.android.data.referral.ReferralMe
import com.trazo.android.data.referral.ReferralRepository
import com.trazo.android.diagnostics.Diagnostics
import com.trazo.android.diagnostics.launchGuarded
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ReferralState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val me: ReferralMe? = null,
    val invitations: List<ReferralInvitation> = emptyList(),
    val grants: List<PremiumGrant> = emptyList(),
    /** The main call (code, link, statistics) failed: nothing useful can be shown. */
    val error: ApiError? = null,
    /** The lists failed but the code is there: shown as a small notice, the code can still be shared. */
    val listError: ApiError? = null
)

/** Everything on this screen comes from the server; nothing is computed here. */
class ReferralViewModel(private val repository: ReferralRepository) : ViewModel() {
    private val _state = MutableStateFlow(ReferralState())
    val state: StateFlow<ReferralState> = _state.asStateFlow()

    init { load() }

    fun load(refresh: Boolean = false) {
        _state.update { it.copy(loading = it.me == null && !refresh, refreshing = refresh, error = null) }
        viewModelScope.launchGuarded(R.string.feature_account, onFailure = {
            _state.update { it.copy(loading = false, refreshing = false) }
        }) {
            // A failure in one endpoint must never cancel its siblings or hide the other results.
            supervisorScope {
                val me = async { loadSafely("ME") { repository.me() } }
                val invitations = async { loadSafely("INVITATIONS") { repository.invitations() } }
                val grants = async { loadSafely("REWARDS") { repository.rewards() } }

                var mainError: ApiError? = null
                when (val result = me.await()) {
                    is ApiResult.Failure -> mainError = result.error
                    is ApiResult.Success -> {
                        val value: Any? = (result as ApiResult.Success<*>).value
                        if (value is ReferralMe) {
                            _state.update { it.copy(me = value) }
                        } else {
                            mainError = unexpectedType("ME", "ReferralMe", value)
                        }
                    }
                }

                var listError: ApiError? = null
                when (val result = invitations.await()) {
                    is ApiResult.Failure -> listError = result.error
                    is ApiResult.Success -> {
                        val value: Any? = (result as ApiResult.Success<*>).value
                        val items = value as? List<*>
                        if (items != null && items.all { it is ReferralInvitation }) {
                            @Suppress("UNCHECKED_CAST")
                            _state.update { it.copy(invitations = items as List<ReferralInvitation>) }
                        } else {
                            listError = unexpectedType("INVITATIONS", "List<ReferralInvitation>", value)
                        }
                    }
                }
                when (val result = grants.await()) {
                    is ApiResult.Failure -> listError = listError ?: result.error
                    is ApiResult.Success -> {
                        val value: Any? = result.value
                        val items = value as? List<*>
                        if (items != null && items.all { it is PremiumGrant }) {
                            @Suppress("UNCHECKED_CAST")
                            _state.update { it.copy(grants = items as List<PremiumGrant>) }
                        } else {
                            listError = listError ?: unexpectedType("REWARDS", "List<PremiumGrant>", value)
                        }
                    }
                }
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        error = mainError,
                        listError = listError
                    )
                }
            }
        }
    }

    private suspend fun <T> loadSafely(
        operation: String,
        request: suspend () -> ApiResult<T>
    ): ApiResult<T> = try {
        request()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Diagnostics.report(IllegalStateException("REFERRAL_VIEWMODEL_${operation}_UNEXPECTED", e))
        ApiResult.Failure(ApiError(com.trazo.android.data.net.ErrorKind.Unknown, code = "referral_${operation.lowercase()}_unexpected"))
    }

    private fun unexpectedType(operation: String, expected: String, actual: Any?): ApiError {
        val actualType = actual?.javaClass?.name ?: "null"
        Diagnostics.report(
            IllegalStateException("REFERRAL_VIEWMODEL_${operation}_TYPE expected=$expected actual=$actualType")
        )
        return ApiError(
            com.trazo.android.data.net.ErrorKind.Unknown,
            code = "referral_${operation.lowercase()}_unexpected_type"
        )
    }

    /** The share sheet was opened: counts it (best effort) and refreshes nothing. */
    fun shared() {
        viewModelScope.launchGuarded(R.string.feature_account) { repository.shared() }
    }
}
