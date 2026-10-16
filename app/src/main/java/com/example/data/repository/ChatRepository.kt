package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(private val db: FirebaseFirestore) {

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

    fun observeConversations(): Flow<List<Conversation>> = flow {
        val uid = requireUserId()
        val path = "conversations"
        db.collection(path)
            .whereArrayContains("memberIds", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(Conversation::class.java).sortedByDescending {
                    it.lastMessageTimestamp ?: it.updatedAt ?: it.createdAt
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun getOrCreateDirectConversation(targetUserId: String, currentUserName: String, targetUserName: String): Result<String> {
        val uid = requireUserId()
        val path = "conversations"
        return try {
            // Check if 1-on-1 direct conversation already exists
            val existing = db.collection(path)
                .whereEqualTo("type", "direct")
                .whereArrayContains("memberIds", uid)
                .get()
                .await()

            val found = existing.toObjects(Conversation::class.java).firstOrNull {
                it.memberIds.contains(targetUserId) && it.memberIds.size == 2
            }

            if (found != null) {
                return Result.success(found.conversationId.ifBlank { found.id })
            }

            // Create new direct conversation
            val convId = "conv_" + UUID.randomUUID().toString().replace("-", "").take(20)
            val docRef = db.collection(path).document(convId)
            val payload = mapOf(
                "conversationId" to convId,
                "type" to "direct",
                "memberIds" to listOf(uid, targetUserId),
                "adminIds" to listOf(uid, targetUserId),
                "title" to targetUserName,
                "lastMessageText" to "Conversation started",
                "lastMessageSenderId" to uid,
                "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(convId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createGroupConversation(
        title: String,
        description: String?,
        memberIds: List<String>
    ): Result<String> {
        val uid = requireUserId()
        val path = "conversations"
        val convId = "group_" + UUID.randomUUID().toString().replace("-", "").take(20)
        val allMembers = (memberIds + uid).distinct()

        val docRef = db.collection(path).document(convId)
        val payload = mutableMapOf<String, Any>(
            "conversationId" to convId,
            "type" to "group",
            "memberIds" to allMembers,
            "adminIds" to listOf(uid),
            "title" to title.trim(),
            "lastMessageText" to "Group created",
            "lastMessageSenderId" to uid,
            "lastMessageTimestamp" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        description?.let { payload["description"] = it.trim() }

        return try {
            docRef.set(payload).await()
            Result.success(convId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = flow {
        val path = "conversations/$conversationId/messages"
        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snap -> snap.toObjects(ChatMessage::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun sendMessage(
        conversationId: String,
        senderName: String,
        memberIds: List<String>,
        text: String,
        messageType: String = "text",
        attachmentUrl: String? = null,
        attachmentName: String? = null,
        attachmentSize: Long? = null,
        attachmentMime: String? = null,
        attachmentRiskScore: String? = null,
        replyToMessageId: String? = null,
        replyToText: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val msgId = "msg_" + UUID.randomUUID().toString().replace("-", "").take(20)
        val msgPath = "conversations/$conversationId/messages/$msgId"

        val payload = mutableMapOf<String, Any>(
            "messageId" to msgId,
            "conversationId" to conversationId,
            "senderId" to uid,
            "senderName" to senderName,
            "messageType" to messageType,
            "text" to text,
            "memberIds" to memberIds,
            "isEdited" to false,
            "deletedForEveryone" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        attachmentUrl?.let { payload["attachmentUrl"] = it }
        attachmentName?.let { payload["attachmentName"] = it }
        attachmentSize?.let { payload["attachmentSize"] = it }
        attachmentMime?.let { payload["attachmentMime"] = it }
        attachmentRiskScore?.let { payload["attachmentRiskScore"] = it }
        replyToMessageId?.let { payload["replyToMessageId"] = it }
        replyToText?.let { payload["replyToText"] = it }

        return try {
            val msgRef = db.collection("conversations").document(conversationId).collection("messages").document(msgId)
            msgRef.set(payload).await()

            // Update conversation last message preview
            val previewText = when (messageType) {
                "image" -> "📷 Photo"
                "video" -> "🎥 Video"
                "apk" -> "📦 APK: " + (attachmentName ?: "Application")
                "document" -> "📄 Document: " + (attachmentName ?: "File")
                "audio" -> "🎤 Voice message"
                else -> text.take(60)
            }
            db.collection("conversations").document(conversationId).update(
                mapOf(
                    "lastMessageText" to previewText,
                    "lastMessageSenderId" to uid,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(msgId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, msgPath)
            Result.failure(e)
        }
    }

    suspend fun editMessage(conversationId: String, messageId: String, newText: String): Result<Unit> {
        val path = "conversations/$conversationId/messages/$messageId"
        return try {
            db.collection("conversations").document(conversationId).collection("messages").document(messageId).update(
                mapOf(
                    "text" to newText,
                    "isEdited" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessageForEveryone(conversationId: String, messageId: String): Result<Unit> {
        val path = "conversations/$conversationId/messages/$messageId"
        return try {
            db.collection("conversations").document(conversationId).collection("messages").document(messageId).update(
                mapOf(
                    "deletedForEveryone" to true,
                    "text" to "This message was deleted",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
