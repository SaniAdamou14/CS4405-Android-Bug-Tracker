package com.example.bugtracker

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bugtracker.data.local.IssuePriority
import com.example.bugtracker.data.local.IssueStatus
import com.example.bugtracker.ui.editor.IssueEditorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IssueEditorStateTest {

    @Test
    fun default_state_has_empty_values() {
        val state = IssueEditorState()

        assertNull(state.issueId)
        assertEquals("", state.title)
        assertEquals("", state.description)
        assertEquals(IssuePriority.MEDIUM, state.priority)
        assertEquals(IssueStatus.OPEN, state.status)
        assertEquals(false, state.isSaving)
        assertNull(state.message)
    }

    @Test
    fun state_preserves_values_after_copy() {
        val original = IssueEditorState(
            issueId = "test-id",
            title = "Bug Title",
            description = "Bug description",
            priority = IssuePriority.CRITICAL,
            status = IssueStatus.IN_PROGRESS
        )

        val updated = original.copy(title = "Updated Title", message = null)

        assertEquals("test-id", updated.issueId)
        assertEquals("Updated Title", updated.title)
        assertEquals("Bug description", updated.description)
        assertEquals(IssuePriority.CRITICAL, updated.priority)
        assertEquals(IssueStatus.IN_PROGRESS, updated.status)
    }

    @Test
    fun saved_state_handle_restores_values() {
        val savedStateHandle = SavedStateHandle()
        val key = "issue_editor_state"
        val state = IssueEditorState(
            title = "Restored Title",
            description = "Restored description",
            priority = IssuePriority.HIGH
        )
        savedStateHandle[key] = state

        val restored = savedStateHandle.get<IssueEditorState>(key)

        assertEquals("Restored Title", restored?.title)
        assertEquals("Restored description", restored?.description)
        assertEquals(IssuePriority.HIGH, restored?.priority)
    }
}
