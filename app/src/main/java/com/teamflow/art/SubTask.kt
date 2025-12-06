package com.teamflow.art

data class SubTask(
    val id: String? = null,
    val taskId: String? = null,
    val projectId: String? = null,

    val title: String? = null,
    val isCompleted: Boolean? = false,
    val assigneeUid: String? = null,
    
    // Time duration in hours
    val durationHours: Int? = null,

    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    
    // Collaborators - multiple users can be assigned
    val collaborators: Map<String, Boolean>? = null,  // uid -> true
    val isSynced: Boolean? = true  // For offline sync tracking
)
