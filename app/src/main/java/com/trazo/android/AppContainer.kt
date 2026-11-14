package com.trazo.android

import android.app.Application
import android.content.Context
import com.trazo.android.auth.AuthApi
import com.trazo.android.auth.AuthRepository
import com.trazo.android.auth.SecureTokenStore
import com.trazo.android.data.api.ExercisesApi
import com.trazo.android.data.api.PlansApi
import com.trazo.android.data.api.SubscriptionApi
import com.trazo.android.data.local.JsonFileStore
import com.trazo.android.data.local.PendingLogQueue
import com.trazo.android.data.local.PlanCache
import com.trazo.android.data.local.WorkoutDraftStore
import com.trazo.android.data.net.NetworkModule
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
    private val cacheStore = JsonFileStore(File(appContext.cacheDir, "plan"), network.gson)
    private val queueStore = JsonFileStore(File(appContext.filesDir, "pending"), network.gson)
    private val draftStore = JsonFileStore(File(appContext.filesDir, "drafts"), network.gson)
    val drafts = WorkoutDraftStore(draftStore)

    val auth = AuthRepository(network.create(AuthApi::class.java), tokens)
    val plans = PlanRepository(network.create(PlansApi::class.java), PlanCache(cacheStore), PendingLogQueue(queueStore), drafts::clear)
    val exercises = ExerciseRepository(network.create(ExercisesApi::class.java))
    val subscription = SubscriptionRepository(network.create(SubscriptionApi::class.java))

    /** Created on first use: nothing talks to Google Play while the backend does not require a subscription. */
    val billing: BillingRepository by lazy { PlayBillingRepository(appContext, subscription, BuildConfig.BILLING_PRODUCT_ID) }

    /** Ends the session and removes everything stored for the account: token, cached week, queued logs and drafts. */
    suspend fun signOut() {
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
    }
}
