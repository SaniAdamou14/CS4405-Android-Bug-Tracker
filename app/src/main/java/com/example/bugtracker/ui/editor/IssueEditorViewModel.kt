package com.example.bugtracker.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bugtracker.data.local.IssuePriority
import com.example.bugtracker.data.local.IssueStatus
import com.example.bugtracker.data.repository.IssueRepository
import com.example.bugtracker.sync.SyncScheduler
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class IssueEditorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: IssueRepository,
    private val scheduler: SyncScheduler
) : ViewModel() {

    private val key = "issue_editor_state"

    val state: StateFlow<IssueEditorState> =
        savedStateHandle.getStateFlow(key, IssueEditorState())

    fun updateTitle(value: String) {
        savedStateHandle[key] = state.value.copy(title = value, message = null)
    }

    fun updateDescription(value: String) {
        savedStateHandle[key] = state.value.copy(description = value, message = null)
    }

    fun updatePriority(value: IssuePriority) {
        savedStateHandle[key] = state.value.copy(priority = value, message = null)
    }

    fun updateStatus(value: IssueStatus) {
        savedStateHandle[key] = state.value.copy(status = value, message = null)
    }

    fun loadIssue(issueId: String) {
        viewModelScope.launch {
            val issue = repository.getIssue(issueId)
            if (issue == null) {
                savedStateHandle[key] = state.value.copy(
                    message = "Issue not found."
                )
                return@launch
            }
            savedStateHandle[key] = IssueEditorState(
                issueId = issue.id,
                title = issue.title,
                description = issue.description,
                priority = issue.priority,
                status = issue.status
            )
        }
    }

    fun saveIssue() {
        val current = state.value

        if (current.title.isBlank()) {
            savedStateHandle[key] = current.copy(
                message = "The issue title is required."
            )
            return
        }
        if (current.title.length > 120) {
            savedStateHandle[key] = current.copy(
                message = "The title cannot exceed 120 characters."
            )
            return
        }
        if (current.description.isBlank()) {
            savedStateHandle[key] = current.copy(
                message = "The issue description is required."
            )
            return
        }
        if (current.description.length > 5000) {
            savedStateHandle[key] = current.copy(
                message = "The description cannot exceed 5,000 characters."
            )
            return
        }

        viewModelScope.launch {
            savedStateHandle[key] = current.copy(isSaving = true, message = null)

            try {
                if (current.issueId == null) {
                    repository.createIssue(
                        title = current.title,
                        description = current.description,
                        priority = current.priority
                    )
                } else {
                    repository.updateIssue(
                        id = current.issueId,
                        title = current.title,
                        description = current.description,
                        priority = current.priority,
                        status = current.status
                    )
                }

                savedStateHandle[key] = IssueEditorState(
                    message = "Issue saved offline."
                )
                scheduler.enqueueSync()
            } catch (e: IllegalArgumentException) {
                savedStateHandle[key] = state.value.copy(
                    isSaving = false,
                    message = e.message ?: "The issue is invalid."
                )
            } catch (e: Exception) {
                savedStateHandle[key] = state.value.copy(
                    isSaving = false,
                    message = "The issue could not be saved."
                )
            }
        }
    }

    fun clearMessage() {
        savedStateHandle[key] = state.value.copy(message = null)
    }
}
