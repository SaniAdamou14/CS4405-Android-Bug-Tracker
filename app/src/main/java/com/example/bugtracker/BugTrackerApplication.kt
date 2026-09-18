package com.example.bugtracker

import android.app.Application
import com.example.bugtracker.data.local.BugTrackerDatabase

class BugTrackerApplication : Application() {

    val database: BugTrackerDatabase by lazy {
        BugTrackerDatabase.getInstance(this)
    }
}
