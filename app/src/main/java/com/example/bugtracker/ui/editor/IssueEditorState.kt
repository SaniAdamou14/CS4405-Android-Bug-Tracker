package com.example.bugtracker.ui.editor

import android.os.Parcelable
import com.example.bugtracker.data.local.IssuePriority
import com.example.bugtracker.data.local.IssueStatus
import kotlinx.parcelize.Parcelize

@Parcelize
data class IssueEditorState(
    val issueId: String? = null,
    val title: String = "",
    val description: String = "",
    val priority: IssuePriority = IssuePriority.MEDIUM,
    val status: IssueStatus = IssueStatus.OPEN,
    val isSaving: Boolean = false,
    val message: String? = null
) : Parcelable
