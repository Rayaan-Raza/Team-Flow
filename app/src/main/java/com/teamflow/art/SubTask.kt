package com.teamflow.art

data class SubTask(
    val id: String? = null,
    val taskId: String? = null,
    val projectId: String? = null,

    val title: String? = null,
    val isCompleted: Boolean? = false,
    val status: String? = "in_progress",  // "in_progress", "done", "completed"
    val assigneeUid: String? = null,
    
    // Time duration in hours
    val durationHours: Int? = null,
    val hours: Int? = null,  // Alternative hours field

    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    
    // Collaborators - multiple users can be assigned
    val collaborators: Map<String, Boolean>? = null,  // uid -> true
    val isSynced: Boolean? = true  // For offline sync tracking
)
