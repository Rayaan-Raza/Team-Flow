package com.teamflow.art

data class ProjectTask(
    var id: String? = null,
    var projectId: String? = null,

    var title: String? = null,
    var description: String? = null,

    var hours: Int? = 0,
    var status: String? = "in_progress",  // in_progress | done

    var dueAt: Long? = null,

    var createdBy: String? = null,
    var createdAt: Long? = null,
    var updatedAt: Long? = null,
    var editedBy: String? = null
)
