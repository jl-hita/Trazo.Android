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
import com.trazo.android.diagnostics.launchGuarded
import kotlinx.coroutines.async
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
        viewModelScope.launchGuarded(R.string.feature_account, onFailure = { _state.update { it.copy(loading = false, refreshing = false) } }) {
            val me = async { repository.me() }
            val invitations = async { repository.invitations() }
            val grants = async { repository.rewards() }
            when (val r = me.await()) {
                is ApiResult.Failure -> { _state.update { it.copy(loading = false, refreshing = false, error = r.error) }; return@launchGuarded }
                is ApiResult.Success -> _state.update { it.copy(me = r.value) }
            }
            var listError: ApiError? = null
            when (val r = invitations.await()) { is ApiResult.Success -> _state.update { it.copy(invitations = r.value) }; is ApiResult.Failure -> listError = r.error }
            when (val r = grants.await()) { is ApiResult.Success -> _state.update { it.copy(grants = r.value) }; is ApiResult.Failure -> listError = listError ?: r.error }
            _state.update { it.copy(loading = false, refreshing = false, listError = listError) }
        }
    }

    /** The share sheet was opened: counts it (best effort) and refreshes nothing. */
    fun shared() {
        viewModelScope.launchGuarded(R.string.feature_account) { repository.shared() }
    }
}
