package com.example.bugtracker.data.remote

import com.example.bugtracker.data.local.IssueEntity
import com.example.bugtracker.data.local.IssuePriority
import com.example.bugtracker.data.local.IssueStatus
import com.example.bugtracker.data.local.PendingOperation
import com.example.bugtracker.data.local.SyncState

fun IssueEntity.toDto(): IssueDto {
    return IssueDto(
        id = id,
        title = title,
        description = description,
        priority = priority.name,
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deleted
    )
}

fun IssueDto.toEntity(): IssueEntity {
    return IssueEntity(
        id = id,
        title = title,
        description = description,
        priority = IssuePriority.valueOf(priority),
        status = IssueStatus.valueOf(status),
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncState = SyncState.SYNCED,
        pendingOperation = PendingOperation.NONE,
        deleted = deleted
    )
}
