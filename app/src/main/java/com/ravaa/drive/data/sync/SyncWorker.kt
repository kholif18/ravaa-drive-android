package com.ravaa.drive.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ravaa.drive.data.repository.DriveRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Background sync: putar outbox + pull terbaru. Jalan saat ada internet. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: DriveRepository
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            repo.sync()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val PERIODIC_TAG = "drive-sync-periodic"
        const val NOW_TAG = "drive-sync-now"

        private fun netConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Sync periodik 15 menit (minimal WorkManager) — dipasang sekali dari Application. */
        fun schedulePeriodic(context: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(netConstraints())
                .addTag(PERIODIC_TAG)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_TAG, ExistingPeriodicWorkPolicy.KEEP, req
            )
        }

        /** Sync sekarang (dipicu refresh manual / internet kembali). */
        fun syncNow(context: Context) {
            val req = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(netConstraints())
                .addTag(NOW_TAG)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                NOW_TAG, ExistingWorkPolicy.REPLACE, req
            )
        }
    }
}
