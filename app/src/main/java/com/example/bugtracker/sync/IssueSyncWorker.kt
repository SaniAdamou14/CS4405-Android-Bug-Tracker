package com.example.bugtracker.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bugtracker.data.repository.IssueRepository
import com.example.bugtracker.data.repository.SyncResult

class IssueSyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val repository: IssueRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting synchronization attempt $runAttemptCount")

        return when (val result = repository.synchronize()) {
            is SyncResult.Success -> {
                Log.d(TAG, "Sync succeeded: pushed=${result.pushedCount}, pulled=${result.pulledCount}")
                Result.success()
            }
            is SyncResult.Failure -> {
                Log.w(TAG, "Sync failed: ${result.message} (retryable=${result.retryable})")
                if (result.retryable && runAttemptCount < MAX_RETRY_ATTEMPTS) {
                    Result.retry()
                } else {
                    Log.e(TAG, "Max retries reached or permanent failure. Keeping pending issues.")
                    Result.failure()
                }
            }
        }
    }

    companion object {
        private const val TAG = "IssueSyncWorker"
        private const val MAX_RETRY_ATTEMPTS = 5
    }
}
