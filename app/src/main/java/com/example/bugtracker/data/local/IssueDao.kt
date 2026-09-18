package com.example.bugtracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface IssueDao {

    @Query(
        """
        SELECT * FROM issues
        WHERE deleted = 0
        ORDER BY updatedAt DESC
        """
    )
    fun observeActiveIssues(): Flow<List<IssueEntity>>

    @Query(
        """
        SELECT * FROM issues
        WHERE id = :issueId
        LIMIT 1
        """
    )
    suspend fun getById(issueId: String): IssueEntity?

    @Query(
        """
        SELECT * FROM issues
        WHERE syncState != 'SYNCED'
        OR pendingOperation != 'NONE'
        ORDER BY updatedAt ASC
        """
    )
    suspend fun getPendingIssues(): List<IssueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(issue: IssueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(issues: List<IssueEntity>)

    @Query(
        """
        UPDATE issues
        SET title = :title,
            description = :description,
            priority = :priority,
            status = :status,
            updatedAt = :updatedAt,
            syncState = 'PENDING',
            pendingOperation = 'UPDATE'
        WHERE id = :issueId
        AND deleted = 0
        """
    )
    suspend fun updateIssue(
        issueId: String,
        title: String,
        description: String,
        priority: IssuePriority,
        status: IssueStatus,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE issues
        SET deleted = 1,
            updatedAt = :updatedAt,
            syncState = 'PENDING',
            pendingOperation = 'DELETE'
        WHERE id = :issueId
        """
    )
    suspend fun markForDeletion(issueId: String, updatedAt: Long): Int

    @Query(
        """
        UPDATE issues
        SET syncState = 'SYNCED',
            pendingOperation = 'NONE'
        WHERE id = :issueId
        """
    )
    suspend fun markAsSynced(issueId: String): Int

    @Query(
        """
        UPDATE issues
        SET syncState = 'FAILED'
        WHERE id = :issueId
        """
    )
    suspend fun markSyncFailed(issueId: String): Int

    @Query(
        """
        DELETE FROM issues
        WHERE id = :issueId
        """
    )
    suspend fun deletePermanently(issueId: String): Int

    @Transaction
    suspend fun replaceRemoteIssues(remoteIssues: List<IssueEntity>) {
        remoteIssues.forEach { remoteIssue ->
            val localIssue = getById(remoteIssue.id)
            val shouldReplace = localIssue == null ||
                (
                    localIssue.pendingOperation == PendingOperation.NONE &&
                    remoteIssue.updatedAt >= localIssue.updatedAt
                )
            if (shouldReplace) {
                upsert(remoteIssue)
            }
        }
    }
}
