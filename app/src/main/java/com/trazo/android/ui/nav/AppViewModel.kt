package com.trazo.android.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trazo.android.auth.AuthRepository
import com.trazo.android.auth.UserSummary
import com.trazo.android.data.model.SubscriptionStatus
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.SessionEvent
import com.trazo.android.data.net.SessionEvents
import com.trazo.android.data.repo.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AppState {
    data object Loading : AppState
    data object SignedOut : AppState

    /** [user] is null when the session was restored offline. */
    data class Ready(val user: UserSummary?, val status: SubscriptionStatus?) : AppState
}

/** Session state of the whole app: loading, signed out or the main navigation. Premium never blocks the app: free plans work without it. */
class AppViewModel(
    private val auth: AuthRepository,
    private val subscription: SubscriptionRepository,
    events: SessionEvents,
    private val signOutAndClear: suspend () -> Unit,
    private val clearLocalData: () -> Unit
) : ViewModel() {
    private val _state = MutableStateFlow<AppState>(AppState.Loading)
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch { events.events.collect(::onEvent) }
        bootstrap()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            if (!auth.hasStoredSession()) { _state.value = AppState.SignedOut; return@launch }
            val restored = auth.restoreSession()
            if (!restored) {
                // Refresh failed for good (tokens cleared) → sign in again; failed only because we are offline → keep going.
                _state.value = if (auth.hasStoredSession()) AppState.Ready(null, null) else AppState.SignedOut
                return@launch
            }
            enter()
        }
    }

    /** Called right after a successful sign-in. */
    fun onSignedIn() {
        _state.value = AppState.Loading
        viewModelScope.launch { enter() }
    }

    private suspend fun enter() {
        val user = runCatching { auth.currentUser() }.getOrNull()
        val status = (subscription.status() as? ApiResult.Success)?.value
        _state.value = AppState.Ready(user, status)
    }

    fun signOut() {
        viewModelScope.launch {
            signOutAndClear()
            _state.value = AppState.SignedOut
        }
    }

    private suspend fun onEvent(event: SessionEvent) {
        when (event) {
            SessionEvent.SessionExpired -> { clearLocalData(); _state.value = AppState.SignedOut }
        }
    }
}
