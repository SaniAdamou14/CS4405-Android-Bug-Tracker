package com.example.bugtracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [IssueEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(IssueConverters::class)
abstract class BugTrackerDatabase : RoomDatabase() {

    abstract fun issueDao(): IssueDao

    companion object {
        @Volatile
        private var instance: BugTrackerDatabase? = null

        fun getInstance(context: Context): BugTrackerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BugTrackerDatabase::class.java,
                    "bug_tracker.db"
                ).build().also {
                    instance = it
                }
            }
        }
    }
}
