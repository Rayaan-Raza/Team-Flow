package com.teamflow.art

data class Project(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val createdBy: String? = null,
    val status: String? = "in_progress",  // backend only
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val dueAt: Long? = null,

    val tasksTotal: Int? = 1,
    val tasksDone: Int? = 0
)
