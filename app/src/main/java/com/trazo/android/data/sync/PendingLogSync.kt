package com.trazo.android.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.trazo.android.TrazoApp
import java.util.concurrent.TimeUnit

/** Asks the system to deliver the queued workout logs later, without the app being open. */
interface SyncScheduler {
    fun schedule()
    fun cancel()

    object None : SyncScheduler {
        override fun schedule() = Unit
        override fun cancel() = Unit
    }
}

class WorkManagerSyncScheduler(context: Context) : SyncScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    /** Runs as soon as there is a connection; if the server cannot be reached the system retries with growing delays. */
    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<PendingLogSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        // KEEP: a run already waiting (or backing off) will also send the log that was just queued.
        workManager.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
    }

    override fun cancel() { workManager.cancelUniqueWork(UNIQUE_NAME) }

    private companion object {
        const val UNIQUE_NAME = "pending-log-sync"
        const val BACKOFF_SECONDS = 30L
    }
}

/** Delivers the queued workout logs. Retries while some are still waiting, so a log is never left behind. */
class PendingLogSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val plans = (applicationContext as TrazoApp).container.plans
        plans.flushPending()
        return if (plans.pendingCount.value == 0) Result.success() else Result.retry()
    }
}
