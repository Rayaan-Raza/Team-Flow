package com.teamflow.art


data class ProjectTaskItem(
    val id: String = "",
    val title: String = "",
    val hours: Int = 0,
    val status: String = "in_progress"
)