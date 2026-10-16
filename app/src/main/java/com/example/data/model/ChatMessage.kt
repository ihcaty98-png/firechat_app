package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val messageType: String = "text", // "text", "image", "video", "audio", "document", "apk", "link"
    val text: String = "",
    val attachmentUrl: String? = null,
    val attachmentName: String? = null,
    val attachmentSize: Long? = null,
    val attachmentMime: String? = null,
    val attachmentRiskScore: String? = null, // "safe", "warning", "caution", "danger", "unverified"
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val isEdited: Boolean = false,
    val deletedForEveryone: Boolean = false,
    val memberIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
