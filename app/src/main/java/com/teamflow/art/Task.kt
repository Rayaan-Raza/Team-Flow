package com.teamflow.art

data class Task(
    val id: String? = null,
    val projectId: String? = null,

    val title: String? = null,
    val description: String? = null,
    val isCompleted: Boolean? = false,
    val priority: String? = null,  // "low", "medium", "high"
    val dueDate: String? = null,
    val assigneeUid: String? = null,
    
    // Time duration in hours
    val durationHours: Int? = null,

    val createdAt: Long? = null,
    val updatedAt: Long? = null,

    val subTaskIds: Map<String, Boolean>? = null,

    val commentsCount: Int? = 0,
    val attachmentsCount: Int? = 0,
    
    // Collaborators - multiple users can be assigned
    val collaborators: Map<String, Boolean>? = null,  // uid -> true
    val conversationId: String? = null,  // For messaging
    val isSynced: Boolean? = true  // For offline sync tracking
)
