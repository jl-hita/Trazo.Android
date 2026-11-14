package com.trazo.android.data.net

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

enum class SessionEvent {
    /** The refresh token is no longer valid: the user has to sign in again. */
    SessionExpired,

    /** The API answered 402: payment is required and the account has no access. */
    SubscriptionRequired
}

/** Lets the network layer tell the UI about session-wide conditions without knowing about screens. */
class SessionEvents {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<SessionEvent> = _events
    fun emit(event: SessionEvent) { _events.tryEmit(event) }
}
