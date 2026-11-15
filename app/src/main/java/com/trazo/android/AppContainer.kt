package com.trazo.android

import android.app.Application
import android.content.Context
import com.trazo.android.auth.AuthApi
import com.trazo.android.auth.AuthRepository
import com.trazo.android.auth.SecureTokenStore
import com.trazo.android.data.api.ExercisesApi
import com.trazo.android.data.api.PlansApi
import com.trazo.android.data.api.SubscriptionApi
import com.trazo.android.data.repo.AnalyticsRepository
import com.trazo.android.data.local.JsonFileStore
import com.trazo.android.data.local.KeystoreKey
import com.trazo.android.data.sync.WorkManagerSyncScheduler
import com.trazo.android.push.PushApi
import com.trazo.android.push.PushConfigStore
import com.trazo.android.push.PushRegistrar
import com.trazo.android.push.SharedPreferencesPushConfigStore
import com.trazo.android.data.local.PendingLogQueue
import com.trazo.android.data.local.PlanCache
import com.trazo.android.data.local.WorkoutDraftStore
import com.trazo.android.data.net.NetworkModule
import com.trazo.android.diagnostics.Diagnostics
import com.trazo.android.diagnostics.DiagnosticsApi
import com.trazo.android.diagnostics.ErrorReporter
import com.trazo.android.diagnostics.PendingErrorStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.trazo.android.data.net.SessionEvents
import com.trazo.android.data.repo.BillingRepository
import com.trazo.android.data.repo.ExerciseRepository
import com.trazo.android.data.repo.PlanRepository
import com.trazo.android.data.repo.PlayBillingRepository
import com.trazo.android.data.repo.SubscriptionRepository
import java.io.File

/**
 * Manual dependency injection. The graph is small (one network module, four repositories) and Hilt would add an annotation
 * processor and build time for no gain; every class takes its dependencies in the constructor, so tests build them by hand.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val tokens = SecureTokenStore(appContext)
    val sessionEvents = SessionEvents()

    init {
        val baseUrl = BuildConfig.API_BASE_URL
        require(if (BuildConfig.DEBUG) baseUrl.startsWith("https://") || baseUrl.startsWith("http://") else baseUrl.startsWith("https://")) {
            "Las compilaciones release requieren HTTPS; HTTP solo está permitido en debug."
        }
    }

    private val network = NetworkModule(BuildConfig.API_BASE_URL, tokens, sessionEvents)

    // The week cache lives in cacheDir (the system may drop it); queued logs and drafts must survive, so they use filesDir.
    // All three are encrypted with a Keystore key: they hold the user's training data.
    private val localCipher = KeystoreKey.cipher()
    private val cacheStore = JsonFileStore(File(appContext.cacheDir, "plan"), network.gson, localCipher)
    private val queueStore = JsonFileStore(File(appContext.filesDir, "pending"), network.gson, localCipher)
    private val draftStore = JsonFileStore(File(appContext.filesDir, "drafts"), network.gson, localCipher)
    val drafts = WorkoutDraftStore(draftStore)

    /** Sends crashes and unexpected errors to the backend as anonymous tickets (see [ErrorReporter]). */
    val errorReporter = ErrorReporter(
        PendingErrorStore(File(appContext.filesDir, "error-reports")),
        network.createAnonymous(DiagnosticsApi::class.java),
        BuildConfig.VERSION_NAME,
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )

    val auth = AuthRepository(network.create(AuthApi::class.java), tokens)
    val plans = PlanRepository(network.create(PlansApi::class.java), PlanCache(cacheStore), PendingLogQueue(queueStore), clearDrafts = drafts::clear, sync = WorkManagerSyncScheduler(appContext))
    val exercises = ExerciseRepository(network.create(ExercisesApi::class.java), BuildConfig.API_BASE_URL)
    val subscription = SubscriptionRepository(network.create(SubscriptionApi::class.java))
    val analytics = AnalyticsRepository(network.create(com.trazo.android.data.api.AnalyticsApi::class.java))

    /** Created on first use: nothing talks to Google Play while the backend does not require a subscription. */
    val billing: BillingRepository by lazy { PlayBillingRepository(appContext, subscription, BuildConfig.BILLING_PRODUCT_ID) }

    /** Push notifications: Firebase is started with options the API serves (set by the superadmin), not built into the app. */
    val pushConfig: PushConfigStore = SharedPreferencesPushConfigStore(appContext)
    val push = PushRegistrar(appContext, network.create(PushApi::class.java), pushConfig)
    private val pushScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Used by the messaging service when Firebase issues a new token while the app is not in the foreground. */
    fun registerPushIfSignedIn() {
        pushScope.launch { if (auth.hasStoredSession()) runCatching { push.register() } }
    }

    /** Ends the session and removes everything stored for the account: token, cached week, queued logs and drafts. */
    suspend fun signOut() {
        // Before the logout, while the session still authorizes the call: this phone stops receiving this user's notices.
        runCatching { push.unregister() }
        runCatching { auth.logout() }
        tokens.clear()
        plans.clearLocal()
    }
}

class TrazoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Crashes are stored on the spot and sent on the next start; handled-but-unexpected errors come in through Diagnostics.
        container.errorReporter.installCrashHandler()
        Diagnostics.install(container.errorReporter)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.errorReporter.flush()
            container.plans.resumePendingSync()
            // Firebase has to be running before a message or a token refresh wakes the app's service.
            container.push.startFromCache()
        }
    }
}
