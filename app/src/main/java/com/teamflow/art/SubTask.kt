package com.teamflow.art

data class SubTask(
    var id: String? = null,
    var projectId: String? = null,
    var taskId: String? = null,

    var title: String? = null,
    var hours: Int? = 0,
    var status: String? = "in_progress",

    var createdAt: Long? = null,
    var updatedAt: Long? = null
)
