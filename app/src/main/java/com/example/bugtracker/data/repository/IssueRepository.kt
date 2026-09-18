package com.example.bugtracker.data.repository

import android.util.Log
import com.example.bugtracker.data.local.IssueDao
import com.example.bugtracker.data.local.IssueEntity
import com.example.bugtracker.data.local.IssuePriority
import com.example.bugtracker.data.local.IssueStatus
import com.example.bugtracker.data.local.PendingOperation
import com.example.bugtracker.data.local.SyncState
import com.example.bugtracker.data.remote.IssueApi
import com.example.bugtracker.data.remote.toDto
import com.example.bugtracker.data.remote.toEntity
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException

class IssueRepository(
    private val dao: IssueDao,
    private val api: IssueApi
) {

    val issues: Flow<List<IssueEntity>> = dao.observeActiveIssues()

    suspend fun createIssue(
        title: String,
        description: String,
        priority: IssuePriority
    ): IssueEntity {
        validate(title, description)
        val now = System.currentTimeMillis()
        val issue = IssueEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            description = description.trim(),
            priority = priority,
            status = IssueStatus.OPEN,
            createdAt = now,
            updatedAt = now
        )
        dao.upsert(issue)
        return issue
    }

    suspend fun updateIssue(
        id: String,
        title: String,
        description: String,
        priority: IssuePriority,
        status: IssueStatus
    ) {
        validate(title, description)
        require(
            dao.updateIssue(
                id, title.trim(), description.trim(),
                priority, status, System.currentTimeMillis()
            ) == 1
        ) { "Issue not found." }
    }

    suspend fun deleteIssue(id: String) {
        require(dao.markForDeletion(id, System.currentTimeMillis()) == 1) {
            "Issue not found."
        }
    }

    suspend fun getIssue(id: String): IssueEntity? {
        return dao.getById(id)
    }

    suspend fun synchronize(): SyncResult {
        return try {
            val pushedCount = pushPendingChanges()
            val pulledCount = pullRemoteIssues()
            SyncResult.Success(pushedCount, pulledCount)
        } catch (e: IOException) {
            Log.e(TAG, "Network error during sync", e)
            SyncResult.Failure(
                retryable = true,
                message = "Network unavailable",
                cause = e
            )
        } catch (e: HttpException) {
            val retryable = e.code() == 408 || e.code() == 425 ||
                e.code() == 429 || e.code() in 500..599
            Log.e(TAG, "HTTP error ${e.code()} during sync", e)
            SyncResult.Failure(retryable, "HTTP ${e.code()}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sync", e)
            SyncResult.Failure(
                retryable = false,
                message = e.message ?: "Synchronization failed",
                cause = e
            )
        }
    }

    private suspend fun pushPendingChanges(): Int {
        var count = 0
        for (issue in dao.getPendingIssues()) {
            try {
                when (issue.pendingOperation) {
                    PendingOperation.CREATE -> pushCreate(issue)
                    PendingOperation.UPDATE -> pushUpdate(issue)
                    PendingOperation.DELETE -> pushDelete(issue)
                    PendingOperation.NONE -> dao.markAsSynced(issue.id)
                }
                count++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push issue ${issue.id}", e)
            }
        }
        return count
    }

    private suspend fun pushCreate(issue: IssueEntity): Boolean {
        val response = api.createIssue(issue.toDto())
        if (!response.isSuccessful) {
            processFailedResponse(issue.id, response.code())
        }
        val remoteIssue = response.body()
            ?: throw IllegalStateException("Create response body is null.")
        dao.upsert(remoteIssue.toEntity())
        return true
    }

    private suspend fun pushUpdate(issue: IssueEntity): Boolean {
        val response = api.updateIssue(issue.id, issue.toDto())
        if (!response.isSuccessful) {
            processFailedResponse(issue.id, response.code())
        }
        val remoteIssue = response.body()
            ?: throw IllegalStateException("Update response body is null.")
        dao.upsert(remoteIssue.toEntity())
        return true
    }

    private suspend fun pushDelete(issue: IssueEntity): Boolean {
        val response = api.deleteIssue(issue.id)
        if (response.isSuccessful || response.code() == 404) {
            dao.deletePermanently(issue.id)
            return true
        }
        processFailedResponse(issue.id, response.code())
    }

    private suspend fun processFailedResponse(issueId: String, statusCode: Int): Nothing {
        dao.markSyncFailed(issueId)
        throw HttpException(
            retrofit2.Response.error<Unit>(
                statusCode,
                "Synchronization failed.".toResponseBody()
            )
        )
    }

    private suspend fun pullRemoteIssues(): Int {
        val response = api.getIssues()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val remoteIssues = response.body()
            ?: throw IllegalStateException("Server returned empty issue list.")
        val entities = remoteIssues.map { it.toEntity() }
        dao.replaceRemoteIssues(entities)
        return entities.size
    }

    private fun validate(title: String, description: String) {
        require(title.isNotBlank()) { "The issue title cannot be empty." }
        require(title.length <= 120) { "The issue title cannot exceed 120 characters." }
        require(description.isNotBlank()) { "The issue description cannot be empty." }
        require(description.length <= 5000) { "The issue description cannot exceed 5,000 characters." }
    }

    companion object {
        private const val TAG = "IssueRepository"
    }
}
