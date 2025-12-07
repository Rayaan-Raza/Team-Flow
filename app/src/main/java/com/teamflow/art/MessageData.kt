package com.teamflow.art

data class MessageData(
    val id: String? = null,
    val messageId: String? = null,
    val conversationId: String? = null,
    val senderUid: String? = null,
    val receiverUid: String? = null,
    val senderName: String? = null,
    val senderEmail: String? = null,
    val messageText: String? = null,
    val imageBase64: String? = null,  // Base64 encoded image
    val timestamp: Long? = null,
    val isRead: Boolean? = false,
    val isSynced: Boolean? = true  // For offline sync tracking
)
