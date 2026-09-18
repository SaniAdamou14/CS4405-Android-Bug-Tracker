package com.example.bugtracker.data.repository

sealed interface SyncResult {

    data class Success(
        val pushedCount: Int,
        val pulledCount: Int
    ) : SyncResult

    data class Failure(
        val retryable: Boolean,
        val message: String,
        val cause: Throwable? = null
    ) : SyncResult
}
