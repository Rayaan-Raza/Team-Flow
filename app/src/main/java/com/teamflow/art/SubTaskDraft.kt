package com.teamflow.art

data class SubTaskDraft(
    var id: String? = null,
    var title: String = "",
    var hours: Int = 0,
    var status: String = "in_progress"
)