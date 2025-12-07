package com.teamflow.art

data class NotificationData(
    val id: String? = null,
    val uid: String? = null,  // Recipient UID
    val type: String? = null,  // "project_invite", "task_assigned", "message", "task_completed", "subtask_completed" etc.
    val title: String? = null,
    val body: String? = null,
    val senderUid: String? = null,
    val senderName: String? = null,
    val projectId: String? = null,
    val taskId: String? = null,
    val subTaskId: String? = null,  // For subtask notifications
    val timestamp: Long? = null,
    val isRead: Boolean? = false,
    val data: Map<String, String>? = null  // Additional data
)
