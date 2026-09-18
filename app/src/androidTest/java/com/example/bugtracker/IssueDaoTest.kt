package com.example.bugtracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bugtracker.data.local.BugTrackerDatabase
import com.example.bugtracker.data.local.IssueDao
import com.example.bugtracker.data.local.IssueEntity
import com.example.bugtracker.data.local.IssuePriority
import com.example.bugtracker.data.local.IssueStatus
import com.example.bugtracker.data.local.PendingOperation
import com.example.bugtracker.data.local.SyncState
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IssueDaoTest {

    private lateinit var db: BugTrackerDatabase
    private lateinit var dao: IssueDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BugTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.issueDao()
    }

    @After
    fun close() = db.close()

    private fun createTestIssue(
        id: String = UUID.randomUUID().toString(),
        title: String = "Test Issue",
        syncState: SyncState = SyncState.PENDING,
        pendingOperation: PendingOperation = PendingOperation.CREATE,
        deleted: Boolean = false,
        updatedAt: Long = System.currentTimeMillis()
    ) = IssueEntity(
        id = id,
        title = title,
        description = "Test description",
        priority = IssuePriority.HIGH,
        status = IssueStatus.OPEN,
        createdAt = System.currentTimeMillis(),
        updatedAt = updatedAt,
        syncState = syncState,
        pendingOperation = pendingOperation,
        deleted = deleted
    )

    @Test
    fun insert_and_read_by_id() = runTest {
        val issue = createTestIssue(id = "test-1", title = "Insert Test")
        dao.upsert(issue)

        val result = dao.getById("test-1")

        assertNotNull(result)
        assertEquals("Insert Test", result!!.title)
    }

    @Test
    fun observe_excludes_deleted_issues() = runTest {
        dao.upsert(createTestIssue(id = "active-1", title = "Active"))
        dao.upsert(createTestIssue(id = "deleted-1", title = "Deleted", deleted = true))

        val issues = dao.observeActiveIssues().first()

        assertEquals(1, issues.size)
        assertEquals("Active", issues[0].title)
    }

    @Test
    fun update_sets_pending_operation() = runTest {
        dao.upsert(createTestIssue(id = "update-1"))

        val rows = dao.updateIssue(
            id = "update-1",
            title = "Updated Title",
            description = "Updated description",
            priority = IssuePriority.LOW,
            status = IssueStatus.IN_PROGRESS,
            updatedAt = System.currentTimeMillis()
        )

        assertEquals(1, rows)
        val issue = dao.getById("update-1")
        assertNotNull(issue)
        assertEquals("Updated Title", issue!!.title)
        assertEquals(SyncState.PENDING, issue.syncState)
        assertEquals(PendingOperation.UPDATE, issue.pendingOperation)
    }

    @Test
    fun mark_for_deletion_sets_delete_operation() = runTest {
        dao.upsert(createTestIssue(id = "delete-1"))

        val rows = dao.markForDeletion("delete-1", System.currentTimeMillis())

        assertEquals(1, rows)
        val issue = dao.getById("delete-1")
        assertNotNull(issue)
        assertEquals(true, issue!!.deleted)
        assertEquals(PendingOperation.DELETE, issue.pendingOperation)
    }

    @Test
    fun get_pending_issues_returns_unsynced() = runTest {
        dao.upsert(createTestIssue(id = "synced-1", syncState = SyncState.SYNCED, pendingOperation = PendingOperation.NONE))
        dao.upsert(createTestIssue(id = "pending-1", syncState = SyncState.PENDING))
        dao.upsert(createTestIssue(id = "failed-1", syncState = SyncState.FAILED))

        val pending = dao.getPendingIssues()

        assertEquals(2, pending.size)
        assertTrue(pending.any { it.id == "pending-1" })
        assertTrue(pending.any { it.id == "failed-1" })
    }

    @Test
    fun mark_as_synced_clears_pending() = runTest {
        dao.upsert(createTestIssue(id = "sync-1", syncState = SyncState.PENDING, pendingOperation = PendingOperation.CREATE))

        dao.markAsSynced("sync-1")

        val issue = dao.getById("sync-1")
        assertNotNull(issue)
        assertEquals(SyncState.SYNCED, issue!!.syncState)
        assertEquals(PendingOperation.NONE, issue.pendingOperation)
    }

    @Test
    fun delete_permanently_removes_row() = runTest {
        dao.upsert(createTestIssue(id = "perm-1"))

        dao.deletePermanently("perm-1")

        val issue = dao.getById("perm-1")
        assertNull(issue)
    }

    @Test
    fun upsert_all_inserts_multiple() = runTest {
        val issues = listOf(
            createTestIssue(id = "multi-1", title = "Issue 1"),
            createTestIssue(id = "multi-2", title = "Issue 2"),
            createTestIssue(id = "multi-3", title = "Issue 3")
        )

        dao.upsertAll(issues)

        val all = dao.observeActiveIssues().first()
        assertEquals(3, all.size)
    }
}
