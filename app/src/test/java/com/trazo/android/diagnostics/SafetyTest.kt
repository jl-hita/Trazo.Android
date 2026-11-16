package com.trazo.android.diagnostics

import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.IOException

/** An unexpected failure must not end the app, must say which function is unavailable, and must always become a ticket. */
class SafetyTest {
    @get:Rule val folder = TemporaryFolder()

    private val sent = mutableListOf<ErrorReportBody>()
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    @Before fun setUp() {
        val api = object : DiagnosticsApi { override suspend fun report(body: ErrorReportBody): Response<Unit> { sent += body; return Response.success(Unit) } }
        val store = PendingErrorStore(folder.newFolder("reports"))
        Diagnostics.install(ErrorReporter(store, api, "9.9.9", CoroutineScope(Job() + Dispatchers.Unconfined)))
    }

    @After fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(defaultHandler)
        Diagnostics.install(null)
    }

    @Test fun `a failing launch is reported and puts the screen back instead of crashing`() = runBlocking {
        var reset = false
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        scope.launchGuarded(feature = 7, onFailure = { reset = true }) { throw IllegalStateException("bug") }.join()
        assertTrue(reset)
        assertEquals(1, sent.size)
        assertTrue(sent.single().stackTrace.startsWith("java.lang.IllegalStateException: bug"))
        assertEquals("9.9.9", sent.single().appVersion)
    }

    @Test fun `guarded reports, runs the reset and lets cancellation through`() = runBlocking {
        var reset = 0
        guarded(onFailure = { reset++ }) { throw NullPointerException("null from the API") }
        assertEquals(1, reset)
        assertEquals(1, sent.size)
        val cancelled = try { guarded { throw kotlinx.coroutines.CancellationException("left") }; false } catch (_: kotlinx.coroutines.CancellationException) { true }
        assertTrue(cancelled)
        assertEquals(1, sent.size)
    }

    @Test fun `a failing reset does not hide the original error`() = runBlocking {
        guarded(onFailure = { throw IllegalStateException("reset failed") }) { throw IllegalArgumentException("first") }
        assertEquals(2, sent.size)
        assertTrue(sent.first().stackTrace.startsWith("java.lang.IllegalArgumentException: first"))
    }

    @Test fun `a broken JVM is not swallowed, it goes to the crash handler`() {
        var crashed: Throwable? = null
        Thread.setDefaultUncaughtExceptionHandler { _, e -> crashed = e }
        handleSurvivable(OutOfMemoryError("heap"), feature = null)
        assertTrue(crashed is OutOfMemoryError)
    }

    @Test fun `the notice for the same function is not repeated within the quiet period`() = runBlocking {
        val seen = mutableListOf<Int>()
        val job = CoroutineScope(Dispatchers.Unconfined).launch { Notices.events.collect { seen += it } }
        Notices.post(feature = 101, now = 1_000)
        Notices.post(feature = 101, now = 2_000)
        Notices.post(feature = 102, now = 2_000)
        Notices.post(feature = 101, now = 1_000 + Notices.QUIET_MILLIS + 1)
        job.cancel()
        assertEquals(listOf(101, 102, 101), seen)
    }

    @Test fun `normal failures get no notice`() = runBlocking {
        val seen = mutableListOf<Int>()
        val job = CoroutineScope(Dispatchers.Unconfined).launch { Notices.events.collect { seen += it } }
        handleSurvivable(IOException("offline"), feature = 201)
        job.cancel()
        assertTrue(seen.isEmpty())
        assertTrue(sent.isEmpty())
    }

    @Test fun `a crash is sent straight from the crash handler and the stored copy is removed`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(204))
            server.start()
            val store = PendingErrorStore(folder.newFolder("crash-reports"))
            Thread.setDefaultUncaughtExceptionHandler { _, _ -> }
            ErrorReporter.installCrashHandler(store, CrashUploader(server.url("/").toString(), "2.0.0"))
            Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), IllegalStateException("startup crash"))

            val request = server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!
            assertEquals("/api/v1/diagnostics/errors", request.path)
            val json = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals("2.0.0", json["appVersion"].asString)
            assertTrue(json["stackTrace"].asString.startsWith("java.lang.IllegalStateException: startup crash"))
            // The handler waited for the upload to settle, so the stored copy is gone (otherwise the next start would send it again).
            assertTrue(store.all().isEmpty())
        }
    }

    @Test fun `a crash that cannot be sent stays stored for the next start`() {
        val store = PendingErrorStore(folder.newFolder("offline-reports"))
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> }
        // Nothing listens on this port: the connection is refused.
        ErrorReporter.installCrashHandler(store, CrashUploader("http://127.0.0.1:1/", "2.0.0"))
        Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), IllegalStateException("offline crash"))
        assertEquals(1, store.all().size)
    }
}
