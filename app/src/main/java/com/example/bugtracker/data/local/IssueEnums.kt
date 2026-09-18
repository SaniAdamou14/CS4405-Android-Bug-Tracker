package com.example.bugtracker.data.local

enum class IssuePriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class IssueStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}

enum class SyncState {
    SYNCED,
    PENDING,
    FAILED
}

enum class PendingOperation {
    NONE,
    CREATE,
    UPDATE,
    DELETE
}
