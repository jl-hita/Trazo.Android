package com.trazo.android.diagnostics

import androidx.annotation.StringRes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * Small notices for failures the app survived. The root of the UI listens and shows a toast saying which function is not
 * available; the same failure is also sent to the backend as a ticket (see [Diagnostics]).
 */
object Notices {
    /** The same function does not announce itself again for this long, so a failing loop does not flood the screen. */
    const val QUIET_MILLIS = 8_000L

    private val _events = MutableSharedFlow<Int>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** The `feature_*` string of the unavailable function, or [GENERIC] when it is not known. */
    val events: SharedFlow<Int> = _events

    const val GENERIC = 0
    private val lastShown = HashMap<Int, Long>()

    fun post(@StringRes feature: Int?, now: Long = System.currentTimeMillis()) {
        val key = feature ?: GENERIC
        val show = synchronized(lastShown) {
            val last = lastShown[key]
            if (last != null && now - last < QUIET_MILLIS) false else { lastShown[key] = now; true }
        }
        if (show) _events.tryEmit(key)
    }
}

/**
 * The single policy for an unexpected error the app can survive: it is reported (stack trace and version go to the backend),
 * [onFailure] puts the screen back in a usable state (no spinner left on), and the user is told which function is unavailable.
 * Only a broken JVM (out of memory, stack overflow) is let through to the crash handler, which reports it and ends the process.
 */
fun handleSurvivable(error: Throwable, @StringRes feature: Int?, onFailure: () -> Unit = {}) {
    if (error is VirtualMachineError) {
        val crash = Thread.getDefaultUncaughtExceptionHandler()
        if (crash != null) { crash.uncaughtException(Thread.currentThread(), error); return }
    }
    Diagnostics.report(error)
    try { onFailure() } catch (inner: Throwable) { Diagnostics.report(inner) }
    // Normal conditions are not bugs: no connection and an ended session already have their own messages.
    if (!ErrorReporter.isExpected(error)) Notices.post(feature)
}

/** Replaces `launch` where a failure must not take the app down (view models, composable scopes, background work). */
fun CoroutineScope.launchGuarded(@StringRes feature: Int? = null, onFailure: () -> Unit = {}, block: suspend CoroutineScope.() -> Unit): Job =
    launch(CoroutineExceptionHandler { _, e -> handleSurvivable(e, feature, onFailure) }, block = block)

/** For code that already runs in a coroutine without a handler of its own (`LaunchedEffect`). Cancellation passes through. */
suspend fun guarded(@StringRes feature: Int? = null, onFailure: () -> Unit = {}, block: suspend () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        handleSurvivable(e, feature, onFailure)
    }
}
