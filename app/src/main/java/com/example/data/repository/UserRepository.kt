package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        val docRef = db.collection("users").document(userId)
        emit(null)
        docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                emit(null)
            }
            .collect { emit(it) }
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile?> {
        val path = "users/$userId"
        return try {
            val doc = db.collection("users").document(userId).get().await()
            val profile = if (doc.exists()) doc.toObject(UserProfile::class.java) else null
            Result.success(profile)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun saveProfile(
        displayName: String,
        username: String,
        bio: String? = null,
        statusMessage: String? = null,
        photoUrl: String? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        val docRef = db.collection("users").document(uid)

        val cleanUsername = username.trim().lowercase()

        return try {
            val existingDoc = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "displayName" to displayName.trim(),
                "username" to cleanUsername,
                "updatedAt" to FieldValue.serverTimestamp(),
                "isOnline" to true
            )
            bio?.let { payload["bio"] = it.trim() }
            statusMessage?.let { payload["statusMessage"] = it.trim() }
            photoUrl?.let { payload["photoUrl"] = it }

            if (!existingDoc.exists()) {
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

    suspend fun updatePresence(isOnline: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val path = "users/$uid"
        return try {
            val updates = mapOf<String, Any>(
                "isOnline" to isOnline,
                "lastSeen" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<List<UserProfile>> {
        val currentUid = auth.currentUser?.uid ?: ""
        val path = "users"
        val q = query.trim().lowercase()
        return try {
            val snapshot = db.collection("users")
                .limit(40)
                .get()
                .await()
            val list = snapshot.toObjects(UserProfile::class.java).filter { user ->
                user.userId != currentUid && (
                    user.username.lowercase().contains(q) ||
                    user.displayName.lowercase().contains(q)
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = flow {
        val currentUid = auth.currentUser?.uid ?: ""
        val path = "users"
        db.collection("users")
            .limit(50)
            .snapshots()
            .map { snap ->
                snap.toObjects(UserProfile::class.java).filter { it.userId != currentUid }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }
}
