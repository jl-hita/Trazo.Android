package com.trazo.android.diagnostics

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.trazo.android.auth.AuthError
import com.trazo.android.auth.AuthException
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.NetworkModule
import com.trazo.android.data.net.SessionEvents
import com.trazo.android.data.net.apiCall
import com.trazo.android.testing.MemoryTokenStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
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

/** Crash and error reporting: what is stored, what is sent, what is never reported, and that the report is anonymous. */
class ErrorReporterTest {
    @get:Rule val folder = TemporaryFolder()

    private class FakeApi(var answer: () -> Response<Unit> = { Response.success(Unit) }) : DiagnosticsApi {
        val sent = mutableListOf<ErrorReportBody>()
        override suspend fun report(body: ErrorReportBody): Response<Unit> { sent += body; return answer() }
    }

    private lateinit var store: PendingErrorStore
    private lateinit var api: FakeApi
    private lateinit var reporter: ErrorReporter
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    @Before fun setUp() {
        store = PendingErrorStore(folder.newFolder("reports"))
        api = FakeApi()
        // Errors are only stored here; the tests flush by hand, so the scope has nothing to run on.
        reporter = ErrorReporter(store, api, "1.2.3+30029", CoroutineScope(Job() + Dispatchers.Unconfined))
    }

    @After fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(defaultHandler)
        Diagnostics.install(null)
    }

    private fun failure(message: String = "boom") = IllegalStateException(message, IllegalArgumentException("root cause"))

    @Test fun `the report carries the complete stack trace with its causes and the app version`() = runBlocking {
        reporter.record(failure())
        assertEquals(1, api.sent.size) // an unexpected error is sent right away
        val body = api.sent.single()
        assertEquals("1.2.3+30029", body.appVersion)
        assertTrue(body.stackTrace.startsWith("java.lang.IllegalStateException: boom"))
        assertTrue(body.stackTrace.contains("Caused by: java.lang.IllegalArgumentException: root cause"))
        assertTrue(body.stackTrace.contains("ErrorReporterTest"))
        assertTrue(store.all().isEmpty()) // sent, so nothing is left on disk
    }

    @Test fun `a missing exception message is explicit in the report header`() {
        val error = ClassCastException()
        error.stackTrace = arrayOf(StackTraceElement("obfuscated.a", "b", "SourceFile", 42))
        val trace = ErrorReporter.traceOf(error)
        assertTrue(trace.startsWith("java.lang.ClassCastException: <no message>"))
        assertTrue(trace.contains("at obfuscated.a.b(SourceFile:42)"))
    }

    @Test fun `only the tester account sees the test buttons`() {
        assertTrue(Diagnostics.isTester("trazostrength@gmail.com"))
        assertTrue(Diagnostics.isTester("TrazoStrength@Gmail.com"))
        assertFalse(Diagnostics.isTester("someone@gmail.com"))
        assertFalse(Diagnostics.isTester("xtrazostrength@gmail.com"))
        assertFalse(Diagnostics.isTester(null))
    }

    @Test fun `a forced test error is sent every time, other errors are still limited`() = runBlocking {
        repeat(3) { reporter.record(failure("test"), force = true) }
        assertEquals(3, api.sent.size)
        reporter.record(IOException("offline"), force = true) // even a test cannot report a normal failure
        assertEquals(3, api.sent.size)
    }

    @Test fun `normal failures are not reported`() = runBlocking {
        reporter.record(IOException("no connection"))
        reporter.record(java.net.UnknownHostException("host"))
        reporter.record(CancellationException("left the screen"))
        reporter.record(AuthException(AuthError.entries.first()))
        assertTrue(api.sent.isEmpty())
        assertTrue(store.all().isEmpty())
    }

    @Test fun `the same error is reported once per run`() = runBlocking {
        repeat(5) { reporter.record(failure()) }
        assertEquals(1, api.sent.size)
    }

    @Test fun `a runaway error source is capped per run`() = runBlocking {
        repeat(ErrorReporter.MAX_NON_FATAL_PER_RUN + 10) { i -> reporter.record(object : RuntimeException("n$i") {}.also { it.stackTrace = arrayOf(StackTraceElement("C$i", "m", "F.kt", 1)) }) }
        assertEquals(ErrorReporter.MAX_NON_FATAL_PER_RUN, api.sent.size)
    }

    @Test fun `a crash is stored by the uncaught handler and sent on the next start`() = runBlocking {
        var delegated: Throwable? = null
        Thread.setDefaultUncaughtExceptionHandler { _, e -> delegated = e } // stands for the system handler that ends the process
        reporter.installCrashHandler()
        val crash = failure("crash")
        Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), crash)

        assertSame(crash, delegated) // the system still gets to handle it
        assertEquals(1, store.all().size)
        assertTrue(api.sent.isEmpty()) // the process is dying: nothing is sent now

        reporter.flush() // the next start
        assertEquals(1, api.sent.size)
        assertTrue(api.sent[0].stackTrace.contains("crash"))
        assertTrue(store.all().isEmpty())
    }

    @Test fun `a repeated crash overwrites one file instead of piling up`() {
        reporter.installCrashHandler()
        val handler = Thread.getDefaultUncaughtExceptionHandler()!!
        repeat(8) { handler.uncaughtException(Thread.currentThread(), failure("crash $it")) }
        assertEquals(1, store.all().size)
    }

    @Test fun `only the newest reports are kept`() {
        repeat(PendingErrorStore.MAX_PENDING + 5) { i ->
            store.save("Error$i: x\n\tat a.B.c$i(B.kt:1)")
            store.all().last().setLastModified(1_000L * (i + 1))
        }
        assertEquals(PendingErrorStore.MAX_PENDING, store.all().size)
    }

    @Test fun `a report that cannot be sent stays on disk and is retried`() = runBlocking {
        api.answer = { throw IOException("offline") }
        reporter.record(failure())
        assertEquals(1, store.all().size)

        api.answer = { Response.error(503, "".toResponseBody()) }
        reporter.flush()
        assertEquals(1, store.all().size)

        api.answer = { Response.error(429, "".toResponseBody()) }
        reporter.flush()
        assertEquals(1, store.all().size)

        api.answer = { Response.success(Unit) }
        reporter.flush()
        assertTrue(store.all().isEmpty())
    }

    @Test fun `a report the server refuses is dropped instead of retried forever`() = runBlocking {
        api.answer = { Response.error(400, "".toResponseBody()) }
        reporter.record(failure())
        assertTrue(store.all().isEmpty())
    }

    @Test fun `the fingerprint ignores line numbers and messages but not the exception type`() {
        val a = PendingErrorStore.fingerprint("java.lang.IllegalStateException: one\n\tat a.B.c(B.kt:10)")
        assertEquals(a, PendingErrorStore.fingerprint("java.lang.IllegalStateException: two\n\tat a.B.c(B.kt:99)"))
        assertNotEquals(a, PendingErrorStore.fingerprint("java.lang.IllegalArgumentException: one\n\tat a.B.c(B.kt:10)"))
    }

    @Test fun `Diagnostics does nothing until installed and never throws`() {
        Diagnostics.report(failure()) // not installed
        Diagnostics.install(reporter)
        Diagnostics.report(failure("through the object"))
        assertEquals(1, api.sent.size)
    }

    @Test fun `an unreadable answer of the API is reported by apiCall but still returns a failure`() = runBlocking {
        Diagnostics.install(reporter)
        val result = apiCall<Unit> { throw com.google.gson.JsonSyntaxException("bad json") }
        assertTrue(result is ApiResult.Failure)
        assertEquals(1, api.sent.size)
        assertTrue(api.sent[0].stackTrace.contains("JsonSyntaxException"))
        // A dropped connection is just a network failure, not a bug.
        assertTrue(apiCall<Unit> { throw IOException("reset") } is ApiResult.Failure)
        assertEquals(1, api.sent.size)
    }

    // ---- over real HTTP ----

    @Test fun `the report is sent anonymously, without the bearer token, as the backend expects`() = runBlocking {
        val server = MockWebServer().also { it.start() }
        try {
            server.enqueue(MockResponse().setResponseCode(202).setBody("""{"accepted":true}"""))
            val tokens = MemoryTokenStore("secret-access", "secret-refresh")
            val anonymous = NetworkModule(server.url("/").toString(), tokens, SessionEvents(), Gson()).createAnonymous(DiagnosticsApi::class.java)
            val real = ErrorReporter(store, anonymous, "1.2.3+30029", CoroutineScope(Job() + Dispatchers.Unconfined))
            real.record(failure("over http"))

            val request = server.takeRequest()
            assertEquals("/api/v1/diagnostics/errors", request.path)
            assertNull(request.getHeader("Authorization"))
            val json = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals(setOf("appVersion", "stackTrace"), json.keySet())
            assertEquals("1.2.3+30029", json["appVersion"].asString)
            assertTrue(json["stackTrace"].asString.contains("over http"))
            real.flush() // waits for the send above to finish (they share a lock) before looking at the disk
            assertTrue(store.all().isEmpty())
        } finally { server.shutdown() }
    }
}
