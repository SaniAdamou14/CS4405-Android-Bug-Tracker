package com.example.bugtracker.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.bugtracker.data.repository.IssueRepository

class BugTrackerWorkerFactory(
    private val repository: IssueRepository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            IssueSyncWorker::class.java.name ->
                IssueSyncWorker(
                    appContext = appContext,
                    params = workerParameters,
                    repository = repository
                )
            else -> null
        }
    }
}
