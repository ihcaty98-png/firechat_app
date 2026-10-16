package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class SecurityEventItem(
    @DocumentId val id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val eventType: String = "",
    val title: String = "",
    val description: String = "",
    val severity: String = "info", // "low", "medium", "high", "critical", "info"
    val remediationStatus: String = "pending", // "resolved", "pending", "action_required"
    @ServerTimestamp val createdAt: Timestamp? = null
)
