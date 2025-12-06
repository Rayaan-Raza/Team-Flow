package com.teamflow.art

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker for syncing offline data
 * Runs periodically to sync pending operations to Firebase
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val syncManager = SyncManager(applicationContext)
            syncManager.syncPendingOperations()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
