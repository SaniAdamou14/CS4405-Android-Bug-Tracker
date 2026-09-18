package com.example.bugtracker.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class IssueDto(
    val id: String,
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false
)
