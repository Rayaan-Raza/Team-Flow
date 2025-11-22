package com.teamflow.art

data class Project(
    val id: String? = null,
    val ownerUid: String? = null,

    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val status: String? = null,
    val priority: String? = null,

    val dueDate: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,

    val attachmentsCount: Int? = 0,
    val commentsCount: Int? = 0,
    val tasksCompleted: Int? = 0,
    val tasksTotal: Int? = 0
)
