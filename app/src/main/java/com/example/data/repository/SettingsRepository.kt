package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.UserSettingsModel
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class SettingsRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun observeUserSettings(): Flow<UserSettingsModel> = flow {
        val uid = requireUserId()
        val path = "users/$uid/settings/main"
        val docRef = db.collection("users").document(uid).collection("settings").document("main")
        docRef.snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(UserSettingsModel::class.java) ?: UserSettingsModel(userId = uid)
                } else {
                    UserSettingsModel(userId = uid)
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                emit(UserSettingsModel(userId = uid))
            }
            .collect { emit(it) }
    }

    suspend fun saveSettings(settings: UserSettingsModel): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/settings/main"
        val docRef = db.collection("users").document(uid).collection("settings").document("main")
        return try {
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "theme" to settings.theme,
                "appLockEnabled" to settings.appLockEnabled,
                "autoLockInactivitySec" to settings.autoLockInactivitySec,
                "notificationsEnabled" to settings.notificationsEnabled,
                "messageSound" to settings.messageSound,
                "readReceipts" to settings.readReceipts,
                "lastSeenVisible" to settings.lastSeenVisible,
                "onlineVisible" to settings.onlineVisible,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            settings.appLockPinHash?.let { payload["appLockPinHash"] = it }

            val snap = docRef.get().await()
            if (!snap.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                docRef.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val uid = requireUserId()
        val path = "users/$uid/settings/main"
        return try {
            val snap = db.collection("users").document(uid).collection("settings").document("main").get().await()
            val savedHash = snap.getString("appLockPinHash") ?: return false
            savedHash == hashPin(pin)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            false
        }
    }
}
