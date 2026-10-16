package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserSettingsModel(
    @DocumentId val id: String = "main",
    val userId: String = "",
    val theme: String = "system", // "system", "light", "dark"
    val appLockEnabled: Boolean = false,
    val appLockPinHash: String? = null,
    val autoLockInactivitySec: Int = 300,
    val notificationsEnabled: Boolean = true,
    val messageSound: Boolean = true,
    val readReceipts: Boolean = true,
    val lastSeenVisible: Boolean = true,
    val onlineVisible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
