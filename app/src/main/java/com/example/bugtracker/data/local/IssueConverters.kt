package com.example.bugtracker.data.local

import androidx.room.TypeConverter

class IssueConverters {

    @TypeConverter
    fun fromPriority(value: IssuePriority): String = value.name

    @TypeConverter
    fun toPriority(value: String): IssuePriority = IssuePriority.valueOf(value)

    @TypeConverter
    fun fromStatus(value: IssueStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): IssueStatus = IssueStatus.valueOf(value)

    @TypeConverter
    fun fromSyncState(value: SyncState): String = value.name

    @TypeConverter
    fun toSyncState(value: String): SyncState = SyncState.valueOf(value)

    @TypeConverter
    fun fromPendingOperation(value: PendingOperation): String = value.name

    @TypeConverter
    fun toPendingOperation(value: String): PendingOperation = PendingOperation.valueOf(value)
}
