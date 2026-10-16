package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val type: String = "direct", // "direct" or "group"
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val title: String? = null,
    val photoUrl: String? = null,
    val description: String? = null,
    val lastMessageText: String? = null,
    val lastMessageSenderId: String? = null,
    val lastMessageTimestamp: Timestamp? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
