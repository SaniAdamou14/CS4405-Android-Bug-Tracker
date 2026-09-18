package com.example.bugtracker

import android.app.Application
import androidx.work.Configuration
import com.example.bugtracker.data.local.BugTrackerDatabase
import com.example.bugtracker.data.remote.ApiProvider
import com.example.bugtracker.data.repository.IssueRepository
import com.example.bugtracker.sync.BugTrackerWorkerFactory

class BugTrackerApplication : Application(), Configuration.Provider {

    val database: BugTrackerDatabase by lazy {
        BugTrackerDatabase.getInstance(this)
    }

    val repository: IssueRepository by lazy {
        IssueRepository(
            dao = database.issueDao(),
            api = ApiProvider.issueApi
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(BugTrackerWorkerFactory(repository))
            .build()
}
