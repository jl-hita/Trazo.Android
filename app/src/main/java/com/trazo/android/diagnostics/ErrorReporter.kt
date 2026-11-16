package com.trazo.android.diagnostics

import com.google.gson.Gson
import com.trazo.android.auth.AuthException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.security.MessageDigest
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** What the backend receives: the app version and the stack trace. Nothing that identifies the user or the device. */
data class ErrorReportBody(val appVersion: String?, val stackTrace: String)

/** Public, anonymous endpoint: it must be called without the bearer token (see `NetworkModule.createAnonymous`). */
interface DiagnosticsApi {
    @POST("api/v1/diagnostics/errors") suspend fun report(@Body body: ErrorReportBody): Response<Unit>
}

/**
 * Reports waiting to be sent, one file per distinct error (the name is a hash of the exception type and the first frames), so a
 * crash loop rewrites one file instead of filling the disk. Writing is plain blocking file I/O on purpose: it runs inside the
 * uncaught-exception handler, moments before the process dies.
 */
class PendingErrorStore(private val dir: File) {
    fun save(trace: String): File? = try {
        dir.mkdirs()
        val file = File(dir, fingerprint(trace) + EXTENSION)
        file.writeText(trace)
        all().dropLast(MAX_PENDING).forEach { it.delete() } // all() is oldest first: keep the newest MAX_PENDING
        file
    } catch (_: Exception) { null /* a report that cannot be stored is lost; the app must not fail because of it */ }

    /** Oldest first. */
    fun all(): List<File> = dir.listFiles { f -> f.name.endsWith(EXTENSION) }?.sortedBy { it.lastModified() }.orEmpty()
    fun read(file: File): String? = try { file.readText() } catch (_: Exception) { null }
    fun delete(file: File) { file.delete() }

    companion object {
        const val EXTENSION = ".trace"
        const val MAX_PENDING = 10

        /** The first line's exception type and the first frames without line numbers: the same bug gives the same key. */
        fun fingerprint(trace: String): String {
            val lines = trace.lineSequence().map { it.trim() }
            val type = lines.firstOrNull().orEmpty().substringBefore(':')
            val frames = trace.lineSequence().map { it.trim() }.filter { it.startsWith("at ") }.take(6).map { it.replace(Regex(":\\d+\\)"), ")") }
            val bytes = MessageDigest.getInstance("SHA-256").digest((listOf(type) + frames).joinToString("\n").toByteArray())
            return bytes.take(8).joinToString("") { "%02x".format(it) }
        }
    }
}

/**
 * Sends a crash straight from the crash handler, without Retrofit or the container (which may be the thing that crashed).
 * Returns true when the report is settled (accepted, or refused for good) and the stored file can go; false to keep it for the next start.
 */
class CrashUploader(private val baseUrl: String, private val appVersion: String?, private val client: OkHttpClient = defaultClient()) {
    fun upload(trace: String): Boolean = try {
        val body = Gson().toJson(ErrorReportBody(appVersion, trace)).toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(baseUrl.trimEnd('/') + "/api/v1/diagnostics/errors").post(body).build()
        client.newCall(request).execute().use { r -> r.isSuccessful || (r.code in 400..499 && r.code != 408 && r.code != 429) }
    } catch (_: Throwable) { false }

    companion object {
        const val TOTAL_MILLIS = 3_500L
        private fun defaultClient() = OkHttpClient.Builder().callTimeout(3, TimeUnit.SECONDS).build()
    }
}

/**
 * Sends every unexpected error to the backend as an anonymous ticket: crashes (uncaught exceptions) are stored at once and
 * sent the next time the app starts; handled-but-unexpected errors are stored and sent right away. A report that could not be
 * sent (offline, server down, rate limit) stays on disk for the next attempt.
 *
 * Errors that are normal, not bugs, are never reported: no connection, a cancelled coroutine, an answer the API gave on purpose.
 */
class ErrorReporter(
    private val store: PendingErrorStore,
    private val api: DiagnosticsApi,
    private val appVersion: String?,
    private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private val reportedThisRun = HashSet<String>()
    private val nonFatalThisRun = AtomicInteger()

    /** Stores the trace of every uncaught exception, then lets the previous handler (the system's) end the process as usual. */
    fun installCrashHandler() = installCrashHandler(store)

    /** An unexpected error the app survived. */
    fun record(error: Throwable, force: Boolean = false) {
        if (isExpected(error)) return
        // The limit comes first: a failure that repeats must not cost a stack trace and a hash every time.
        if (!force && nonFatalThisRun.incrementAndGet() > MAX_NON_FATAL_PER_RUN) return
        val trace = traceOf(error)
        if (!force) {
            val key = PendingErrorStore.fingerprint(trace)
            if (synchronized(reportedThisRun) { !reportedThisRun.add(key) }) return // one report per kind of error and run
        }
        store.save(trace)
        scope.launch { flush() }
    }

    /** Sends what is stored. Stops at the first failure that a retry could fix, so nothing is lost. */
    suspend fun flush() = mutex.withLock {
        for (file in store.all()) {
            val trace = store.read(file)
            if (trace.isNullOrBlank()) { store.delete(file); continue }
            val response = try {
                api.report(ErrorReportBody(appVersion, trace))
            } catch (_: IOException) { return@withLock }
            catch (e: RuntimeException) { if (e is CancellationException) throw e; return@withLock }
            when {
                response.isSuccessful -> store.delete(file)
                // The server will not accept this one (invalid, too large): sending it again would not help.
                response.code() in 400..499 && response.code() != 408 && response.code() != 429 -> store.delete(file)
                else -> return@withLock // rate limited or server trouble: keep the rest for later
            }
        }
    }

    companion object {
        const val MAX_NON_FATAL_PER_RUN = 20
        private const val MAX_TRACE_LENGTH = 16_000

        /**
         * Needs only the store, so the app can call it as the very first thing in `Application.onCreate`: a crash while the rest
         * of the app is being built (Keystore, network layer...) is then stored too and sent on the next start.
         */
        fun installCrashHandler(store: PendingErrorStore, uploader: CrashUploader? = null) {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                try {
                    val trace = traceOf(error)
                    val file = store.save(trace)
                    // Sent right away when there is a connection, so the ticket exists even if the app dies again at the next start.
                    if (uploader != null && file != null) uploadNow(uploader, trace, file, store)
                } catch (_: Throwable) { }
                previous?.uncaughtException(thread, error)
            }
        }

        /** Network off the crashing thread, with a hard limit: the process is dying and must not hang. */
        private fun uploadNow(uploader: CrashUploader, trace: String, file: File, store: PendingErrorStore) {
            val worker = Thread { if (uploader.upload(trace)) store.delete(file) }
            worker.isDaemon = true
            worker.start()
            worker.join(CrashUploader.TOTAL_MILLIS)
        }

        fun isExpected(error: Throwable): Boolean = error is IOException || error is CancellationException || error is AuthException

        /** The complete trace, causes included (the backend scrubs it and keeps up to 16,000 characters). */
        fun traceOf(error: Throwable): String {
            val writer = StringWriter()
            error.printStackTrace(PrintWriter(writer))
            val text = writer.toString().trim()
            return if (text.length <= MAX_TRACE_LENGTH) text else text.take(MAX_TRACE_LENGTH)
        }
    }
}

/** Entry point for code that has no reference to the app container (the network layer, composables). A no-op until installed. */
object Diagnostics {
    @Volatile private var reporter: ErrorReporter? = null

    fun install(reporter: ErrorReporter?) { this.reporter = reporter }

    /** The one account that may provoke errors on purpose to check the ticket system (see the account screen). */
    const val TESTER_EMAIL = "trazostrength@gmail.com"

    fun isTester(email: String?): Boolean = email.equals(TESTER_EMAIL, ignoreCase = true)

    /** [force] skips the once-per-run and per-run limits; only for the deliberate test errors. */
    fun report(error: Throwable, force: Boolean = false) {
        try { reporter?.record(error, force) } catch (_: Throwable) { /* reporting must never fail the code that found the error */ }
    }
}
