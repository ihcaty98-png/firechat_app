package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createConversation_authenticatedUser_success() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val bobUid = "bob_mock_uid"
        val repository = ChatRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrCreateDirectConversation(bobUid, "Alice", "Bob")
        }
        assertTrue(result.isSuccess)
        val convId = result.getOrThrow()
        assertTrue(convId.isNotEmpty())
    }

    @Test
    fun sendMessage_authenticatedMember_success() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val bobUid = "bob_mock_uid"
        val repository = ChatRepository(firestore)

        val convResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrCreateDirectConversation(bobUid, "Alice", "Bob")
        }
        val convId = convResult.getOrThrow()

        val msgResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.sendMessage(
                conversationId = convId,
                senderName = "Alice",
                memberIds = listOf(aliceUid, bobUid),
                text = "Hello Bob from automated test!"
            )
        }
        assertTrue(msgResult.isSuccess)
    }

    @Test
    fun observeConversations_unauthenticated_failsGracefullyOrRequiresAuth() = runBlocking {
        auth.signOut()
        val repository = ChatRepository(firestore)
        var failed = false
        try {
            repository.observeConversations().first()
        } catch (_: IllegalStateException) {
            failed = true
        }
        assertTrue("Repository requireUserId asserted on unauthenticated call", failed)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@chati.test"
        const val BOB_EMAIL = "bob@chati.test"
        const val DEFAULT_TIMEOUT_MS = 6000L
    }
}
