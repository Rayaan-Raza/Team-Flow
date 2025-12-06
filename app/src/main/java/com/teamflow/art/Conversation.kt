package com.teamflow.art

data class Conversation(
    val id: String? = null,
    val conversationId: String? = null,
    val projectId: String? = null,
    val taskId: String? = null,
    val participants: List<String>? = null,
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null,
    val lastSenderUid: String? = null,
    val unreadCount: Int? = 0,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    // Additional fields for inbox display
    val otherUserId: String? = null,
    val otherUserName: String? = null,
    val otherUserEmail: String? = null,
    val hasUnread: Boolean? = false,
    val photoBase64: String? = null
)
