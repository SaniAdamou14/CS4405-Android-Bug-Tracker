package com.example.bugtracker

import com.example.bugtracker.data.local.IssueDao
import com.example.bugtracker.data.local.IssueEntity
import com.example.bugtracker.data.local.IssuePriority
import com.example.bugtracker.data.local.IssueStatus
import com.example.bugtracker.data.local.PendingOperation
import com.example.bugtracker.data.local.SyncState
import com.example.bugtracker.data.remote.IssueApi
import com.example.bugtracker.data.remote.IssueDto
import com.example.bugtracker.data.repository.IssueRepository
import com.example.bugtracker.data.repository.SyncResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class IssueRepositoryTest {

    private lateinit var dao: IssueDao
    private lateinit var api: IssueApi
    private lateinit var repository: IssueRepository

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        api = mockk(relaxed = true)
        repository = IssueRepository(dao, api)

        coEvery { dao.observeActiveIssues() } returns flowOf(emptyList())
    }

    @Test
    fun `createIssue inserts into DAO with PENDING state`() = runTest {
        val issue = repository.createIssue(
            title = "Test Bug",
            description = "Test description",
            priority = IssuePriority.HIGH
        )

        assertNotNull(issue)
        assertEquals("Test Bug", issue.title)
        assertEquals("Test description", issue.description)
        assertEquals(IssuePriority.HIGH, issue.priority)
        assertEquals(IssueStatus.OPEN, issue.status)
        assertEquals(SyncState.PENDING, issue.syncState)
        assertEquals(PendingOperation.CREATE, issue.pendingOperation)
        coVerify { dao.upsert(any()) }
    }

    @Test
    fun `createIssue rejects blank title`() = runTest {
        try {
            repository.createIssue(
                title = "",
                description = "Description",
                priority = IssuePriority.MEDIUM
            )
            throw AssertionError("Should have thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("title") == true)
        }
    }

    @Test
    fun `createIssue rejects blank description`() = runTest {
        try {
            repository.createIssue(
                title = "Title",
                description = "",
                priority = IssuePriority.MEDIUM
            )
            throw AssertionError("Should have thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("description") == true)
        }
    }

    @Test
    fun `deleteIssue marks for deletion`() = runTest {
        coEvery { dao.markForDeletion(any(), any()) } returns 1

        repository.deleteIssue("test-id")

        coVerify { dao.markForDeletion("test-id", any()) }
    }

    @Test
    fun `synchronize returns Success on network call`() = runTest {
        coEvery { dao.getPendingIssues() } returns emptyList()
        coEvery { api.getIssues() } returns Response.success(emptyList())

        val result = repository.synchronize()

        assertTrue(result is SyncResult.Success)
    }

    @Test
    fun `synchronize returns Failure on IOException`() = runTest {
        coEvery { dao.getPendingIssues() } returns emptyList()
        coEvery { api.getIssues() } throws IOException("No network")

        val result = repository.synchronize()

        assertTrue(result is SyncResult.Failure)
        assertEquals(true, (result as SyncResult.Failure).retryable)
    }

    @Test
    fun `synchronize returns retryable Failure on HTTP 500`() = runTest {
        coEvery { dao.getPendingIssues() } returns emptyList()
        val errorBody = "Server Error".toResponseBody("text/plain".toMediaType())
        val response = Response.error<List<IssueDto>>(500, errorBody)
        coEvery { api.getIssues() } throws HttpException(response)

        val result = repository.synchronize()

        assertTrue(result is SyncResult.Failure)
        assertEquals(true, (result as SyncResult.Failure).retryable)
    }

    @Test
    fun `synchronize returns non-retryable Failure on HTTP 400`() = runTest {
        coEvery { dao.getPendingIssues() } returns emptyList()
        val errorBody = "Bad Request".toResponseBody("text/plain".toMediaType())
        val response = Response.error<List<IssueDto>>(400, errorBody)
        coEvery { api.getIssues() } throws HttpException(response)

        val result = repository.synchronize()

        assertTrue(result is SyncResult.Failure)
        assertEquals(false, (result as SyncResult.Failure).retryable)
    }
}
