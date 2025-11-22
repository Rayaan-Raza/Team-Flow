package com.teamflow.art

data class SubTask(
    val id: String? = null,
    val taskId: String? = null,
    val projectId: String? = null,

    val title: String? = null,
    val isCompleted: Boolean? = false,
    val assigneeUid: String? = null,
    val durationMinutes: Int? = null
)