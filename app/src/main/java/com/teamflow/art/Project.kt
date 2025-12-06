package com.teamflow.art

data class Project(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val createdBy: String? = null,
    val status: String? = "in_progress",  // "in_progress", "completed", "archived"
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val dueAt: Long? = null,

    val tasksTotal: Int? = 1,
    val tasksDone: Int? = 0,
    
    // Collaborators (stored separately in Firebase but included here for convenience)
    val collaborators: Map<String, Boolean>? = null,  // uid -> true
    val isSynced: Boolean? = true  // For offline sync tracking
)
