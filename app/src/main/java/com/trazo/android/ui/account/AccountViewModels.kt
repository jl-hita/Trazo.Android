package com.trazo.android.ui.account

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.android.data.model.SubscriptionStatus
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.BillingOffer
import com.trazo.android.data.repo.BillingRepository
import com.trazo.android.data.repo.PlanRepository
import com.trazo.android.data.repo.PurchaseResult
import com.trazo.android.data.repo.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountState(
    val status: SubscriptionStatus? = null,
    val statusError: ApiError? = null,
    val archiving: Boolean = false,
    val archived: Boolean = false,
    val archiveError: ApiError? = null,
    val pendingLogs: Int = 0
)

/** Account tab: subscription state, archiving the plan, and the number of workouts waiting to be sent. */
class AccountViewModel(
    private val subscription: SubscriptionRepository,
    private val plans: PlanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AccountState())
    val state: StateFlow<AccountState> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch { plans.pendingCount.collect { n -> _state.update { it.copy(pendingLogs = n) } } }
    }

    fun load() {
        viewModelScope.launch {
            when (val r = subscription.status()) {
                is ApiResult.Success -> _state.update { it.copy(status = r.value, statusError = null) }
                is ApiResult.Failure -> _state.update { it.copy(statusError = r.error) }
            }
        }
    }

    fun archivePlan() {
        _state.update { it.copy(archiving = true, archiveError = null) }
        viewModelScope.launch {
            when (val r = plans.archive()) {
                is ApiResult.Success -> _state.update { it.copy(archiving = false, archived = true) }
                is ApiResult.Failure -> _state.update { it.copy(archiving = false, archiveError = r.error) }
            }
        }
    }

    fun sendPending() { viewModelScope.launch { plans.flushPending() } }

    fun consumeArchived() = _state.update { it.copy(archived = false) }
}

data class PaywallState(
    val loading: Boolean = true,
    val offer: BillingOffer? = null,
    val unavailable: Boolean = false,
    val purchasing: Boolean = false,
    val pending: Boolean = false,
    val error: ApiError? = null,
    val failed: Boolean = false
)

/**
 * The plans screen. It exists only while the backend says a subscription is required and the account has no access; the
 * app never decides that on its own.
 */
class PaywallViewModel(
    private val billing: BillingRepository,
    private val userId: String?,
    private val onAccess: (SubscriptionStatus) -> Unit
) : ViewModel() {
    private val _state = MutableStateFlow(PaywallState())
    val state: StateFlow<PaywallState> = _state.asStateFlow()

    init {
        viewModelScope.launch { billing.results.collect(::onResult) }
        viewModelScope.launch {
            // A purchase made earlier (or on another device) may only need to be linked to the account again.
            billing.restore()?.let { onResult(it) }
            val offer = billing.loadOffer()
            _state.update { it.copy(loading = false, offer = offer, unavailable = offer == null) }
        }
    }

    fun buy(activity: Activity) {
        val id = userId ?: run { _state.update { it.copy(failed = true) }; return }
        _state.update { it.copy(purchasing = true, failed = false, error = null, pending = false) }
        viewModelScope.launch {
            if (!billing.launch(activity, id)) _state.update { it.copy(purchasing = false, failed = true) }
        }
    }

    fun restore() {
        _state.update { it.copy(purchasing = true, failed = false, error = null) }
        viewModelScope.launch {
            val result = billing.restore()
            if (result == null) _state.update { it.copy(purchasing = false, failed = true) } else onResult(result)
        }
    }

    private fun onResult(result: PurchaseResult) {
        when (result) {
            is PurchaseResult.Verified -> { _state.update { it.copy(purchasing = false) }; onAccess(result.status) }
            PurchaseResult.Pending -> _state.update { it.copy(purchasing = false, pending = true) }
            PurchaseResult.Canceled -> _state.update { it.copy(purchasing = false) }
            is PurchaseResult.Failed -> _state.update { it.copy(purchasing = false, failed = result.error == null, error = result.error) }
        }
    }

    override fun onCleared() { billing.close() }
}
