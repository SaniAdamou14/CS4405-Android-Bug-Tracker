package com.example.bugtracker.sync

import android.content.Context
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
        return when (val result = repository.synchronize()) {
            is SyncResult.Success -> Result.success()
            is SyncResult.Failure -> {
                if (result.retryable) Result.retry() else Result.failure()
            }
        }
    }
}
