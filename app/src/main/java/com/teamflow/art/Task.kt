package com.teamflow.art

data class Task(
    val id: String? = null,
    val projectId: String? = null,

    val title: String? = null,
    val description: String? = null,
    val isCompleted: Boolean? = false,
    val priority: String? = null,
    val dueDate: String? = null,
    val assigneeUid: String? = null,

    val createdAt: Long? = null,
    val updatedAt: Long? = null,

    val subTaskIds: Map<String, Boolean>? = null,

    // NEW
    val commentsCount: Int? = 0,
    val attachmentsCount: Int? = 0
)
