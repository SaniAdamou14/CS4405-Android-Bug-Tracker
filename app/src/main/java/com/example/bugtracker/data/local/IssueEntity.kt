package com.example.bugtracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "issues")
data class IssueEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val priority: IssuePriority,
    val status: IssueStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val syncState: SyncState = SyncState.PENDING,
    val pendingOperation: PendingOperation = PendingOperation.CREATE,
    val deleted: Boolean = false
)
