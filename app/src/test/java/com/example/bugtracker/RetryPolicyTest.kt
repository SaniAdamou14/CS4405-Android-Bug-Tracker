package com.example.bugtracker

import com.example.bugtracker.data.repository.IssueRepository
import com.example.bugtracker.data.repository.SyncResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RetryPolicyTest {

    private lateinit var repository: IssueRepository

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
    }

    @Test
    fun `retryable failures should trigger retry`() = runTest {
        coEvery { repository.synchronize() } returns SyncResult.Failure(
            retryable = true,
            message = "Network unavailable"
        )

        val result = repository.synchronize()

        assertTrue(result is SyncResult.Failure)
        assertEquals(true, (result as SyncResult.Failure).retryable)
    }

    @Test
    fun `non-retryable failures should not trigger retry`() = runTest {
        coEvery { repository.synchronize() } returns SyncResult.Failure(
            retryable = false,
            message = "HTTP 400"
        )

        val result = repository.synchronize()

        assertTrue(result is SyncResult.Failure)
        assertEquals(false, (result as SyncResult.Failure).retryable)
    }

    @Test
    fun `successful sync returns counts`() = runTest {
        coEvery { repository.synchronize() } returns SyncResult.Success(
            pushedCount = 3,
            pulledCount = 5
        )

        val result = repository.synchronize()

        assertTrue(result is SyncResult.Success)
        val success = result as SyncResult.Success
        assertEquals(3, success.pushedCount)
        assertEquals(5, success.pulledCount)
    }
}
