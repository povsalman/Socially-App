package com.salmankhan.i221285.services

import android.util.Log
import com.google.firebase.database.*
import com.salmankhan.i221285.models.Chat
import com.salmankhan.i221285.models.Message
import kotlinx.coroutines.tasks.await

/**
 * Service class for managing Messages and Chats in Firebase Realtime Database
 */
object MessageService {
    private const val TAG = "MessageService"
    private val database = FirebaseDatabase.getInstance()
    private val chatsRef = database.getReference("chats")
    private val messagesRef = database.getReference("messages")
    private val usersRef = database.getReference("users")

    /**
     * Get or create a chat between two users
     */
    suspend fun getOrCreateChat(user1Id: String, user2Id: String): Result<String> {
        return try {
            val chatId = Chat.generateChatId(user1Id, user2Id)
            val chatSnapshot = chatsRef.child(chatId).get().await()

            if (!chatSnapshot.exists()) {
                // Create new chat
                // Sort user IDs to ensure consistent chat ID generation
                val sortedUser1Id = if (user1Id < user2Id) user1Id else user2Id
                val sortedUser2Id = if (user1Id < user2Id) user2Id else user1Id
                
                // Fetch user data
                val user1Snapshot = usersRef.child(sortedUser1Id).get().await()
                val user2Snapshot = usersRef.child(sortedUser2Id).get().await()

                val chat = mapOf(
                    "id" to chatId,
                    "user1Id" to sortedUser1Id,
                    "user2Id" to sortedUser2Id,
                    "user1Name" to (user1Snapshot.child("username").getValue(String::class.java) ?: "User"),
                    "user2Name" to (user2Snapshot.child("username").getValue(String::class.java) ?: "User"),
                    "user1ProfileImage" to (user1Snapshot.child("profileImage").getValue(String::class.java) ?: ""),
                    "user2ProfileImage" to (user2Snapshot.child("profileImage").getValue(String::class.java) ?: ""),
                    "lastMessage" to "",
                    "lastMessageTime" to System.currentTimeMillis(),
                    "unreadCount_user1" to 0,
                    "unreadCount_user2" to 0
                )

                chatsRef.child(chatId).setValue(chat).await()
                Log.d(TAG, "Created new chat: $chatId")
            }

            Result.success(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting/creating chat: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send a text message
     */
    suspend fun sendTextMessage(chatId: String, senderId: String, receiverId: String, text: String): Result<Unit> {
        return try {
            val messageId = messagesRef.child(chatId).push().key ?: return Result.failure(Exception("Failed to generate message ID"))

            val message = mapOf(
                "id" to messageId,
                "chatId" to chatId,
                "senderId" to senderId,
                "receiverId" to receiverId,
                "type" to "text",
                "content" to text,
                "postId" to "",
                "createdAt" to System.currentTimeMillis(),
                "editedAt" to 0L,
                "isEdited" to false,
                "isDeleted" to false
            )

            messagesRef.child(chatId).child(messageId).setValue(message).await()
            updateChatLastMessage(chatId, text, senderId, receiverId)

            // Send push notification
            val senderSnapshot = usersRef.child(senderId).get().await()
            val senderUsername = senderSnapshot.child("username").getValue(String::class.java) ?: "Someone"
            NotificationHelper.sendMessageNotification(receiverId, senderUsername, text)

            Log.d(TAG, "Sent text message in chat: $chatId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending text message: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send an image message
     */
    suspend fun sendImageMessage(chatId: String, senderId: String, receiverId: String, imageBase64: String): Result<Unit> {
        return try {
            val messageId = messagesRef.child(chatId).push().key ?: return Result.failure(Exception("Failed to generate message ID"))

            val message = mapOf(
                "id" to messageId,
                "chatId" to chatId,
                "senderId" to senderId,
                "receiverId" to receiverId,
                "type" to "image",
                "content" to imageBase64,
                "postId" to "",
                "createdAt" to System.currentTimeMillis(),
                "editedAt" to 0L,
                "isEdited" to false,
                "isDeleted" to false
            )

            messagesRef.child(chatId).child(messageId).setValue(message).await()
            updateChatLastMessage(chatId, "📷 Photo", senderId, receiverId)

            Log.d(TAG, "Sent image message in chat: $chatId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending image message: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send a post message
     */
    suspend fun sendPostMessage(chatId: String, senderId: String, receiverId: String, postId: String): Result<Unit> {
        return try {
            val messageId = messagesRef.child(chatId).push().key ?: return Result.failure(Exception("Failed to generate message ID"))

            val message = mapOf(
                "id" to messageId,
                "chatId" to chatId,
                "senderId" to senderId,
                "receiverId" to receiverId,
                "type" to "post",
                "content" to "",
                "postId" to postId,
                "createdAt" to System.currentTimeMillis(),
                "editedAt" to 0L,
                "isEdited" to false,
                "isDeleted" to false
            )

            messagesRef.child(chatId).child(messageId).setValue(message).await()
            updateChatLastMessage(chatId, "📄 Post", senderId, receiverId)

            Log.d(TAG, "Sent post message in chat: $chatId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending post message: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Edit a message (only if within 5 minutes)
     */
    suspend fun editMessage(chatId: String, messageId: String, newText: String): Result<Unit> {
        return try {
            val messageSnapshot = messagesRef.child(chatId).child(messageId).get().await()
            if (!messageSnapshot.exists()) {
                return Result.failure(Exception("Message not found"))
            }

            val createdAt = messageSnapshot.child("createdAt").getValue(Long::class.java) ?: 0L
            val message = Message(
                id = messageId,
                chatId = chatId,
                createdAt = createdAt
            )

            if (!message.canEditOrDelete()) {
                return Result.failure(Exception("Cannot edit message after 5 minutes"))
            }

            val updates = mapOf(
                "content" to newText,
                "editedAt" to System.currentTimeMillis(),
                "isEdited" to true
            )

            messagesRef.child(chatId).child(messageId).updateChildren(updates).await()
            Log.d(TAG, "Edited message: $messageId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error editing message: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a message (only if within 5 minutes)
     */
    suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> {
        return try {
            val messageSnapshot = messagesRef.child(chatId).child(messageId).get().await()
            if (!messageSnapshot.exists()) {
                return Result.failure(Exception("Message not found"))
            }

            val createdAt = messageSnapshot.child("createdAt").getValue(Long::class.java) ?: 0L
            val message = Message(
                id = messageId,
                chatId = chatId,
                createdAt = createdAt
            )

            if (!message.canEditOrDelete()) {
                return Result.failure(Exception("Cannot delete message after 5 minutes"))
            }

            val updates = mapOf(
                "content" to "",
                "isDeleted" to true
            )

            messagesRef.child(chatId).child(messageId).updateChildren(updates).await()
            Log.d(TAG, "Deleted message: $messageId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all messages for a chat
     */
    fun getChatMessages(chatId: String, callback: (List<Message>) -> Unit) {
        messagesRef.child(chatId).orderByChild("createdAt")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = mutableListOf<Message>()
                    for (child in snapshot.children) {
                        try {
                            val message = Message(
                                id = child.child("id").getValue(String::class.java) ?: "",
                                chatId = child.child("chatId").getValue(String::class.java) ?: "",
                                senderId = child.child("senderId").getValue(String::class.java) ?: "",
                                receiverId = child.child("receiverId").getValue(String::class.java) ?: "",
                                type = child.child("type").getValue(String::class.java) ?: "text",
                                content = child.child("content").getValue(String::class.java) ?: "",
                                postId = child.child("postId").getValue(String::class.java) ?: "",
                                createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L,
                                editedAt = child.child("editedAt").getValue(Long::class.java) ?: 0L,
                                isEdited = child.child("isEdited").getValue(Boolean::class.java) ?: false,
                                isDeleted = child.child("isDeleted").getValue(Boolean::class.java) ?: false
                            )
                            messages.add(message)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing message: ${e.message}", e)
                        }
                    }
                    callback(messages)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading messages: ${error.message}")
                    callback(emptyList())
                }
            })
    }

    /**
     * Get all chats for a user
     */
    fun getUserChats(userId: String, callback: (List<Chat>) -> Unit) {
        chatsRef.orderByChild("lastMessageTime")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val chats = mutableListOf<Chat>()
                    for (child in snapshot.children) {
                        try {
                            val user1Id = child.child("user1Id").getValue(String::class.java) ?: ""
                            val user2Id = child.child("user2Id").getValue(String::class.java) ?: ""

                            // Only include chats where this user is a participant
                            if (user1Id == userId || user2Id == userId) {
                                val chat = Chat(
                                    id = child.child("id").getValue(String::class.java) ?: "",
                                    user1Id = user1Id,
                                    user2Id = user2Id,
                                    user1Name = child.child("user1Name").getValue(String::class.java) ?: "",
                                    user2Name = child.child("user2Name").getValue(String::class.java) ?: "",
                                    user1ProfileImage = child.child("user1ProfileImage").getValue(String::class.java) ?: "",
                                    user2ProfileImage = child.child("user2ProfileImage").getValue(String::class.java) ?: "",
                                    lastMessage = child.child("lastMessage").getValue(String::class.java) ?: "",
                                    lastMessageTime = child.child("lastMessageTime").getValue(Long::class.java) ?: 0L,
                                    unreadCount_user1 = child.child("unreadCount_user1").getValue(Int::class.java) ?: 0,
                                    unreadCount_user2 = child.child("unreadCount_user2").getValue(Int::class.java) ?: 0
                                )
                                chats.add(chat)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing chat: ${e.message}", e)
                        }
                    }
                    // Reverse to show most recent first
                    callback(chats.reversed())
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading chats: ${error.message}")
                    callback(emptyList())
                }
            })
    }

    /**
     * Update chat's last message
     */
    private fun updateChatLastMessage(chatId: String, message: String, senderId: String, receiverId: String) {
        val updates = mapOf(
            "lastMessage" to message,
            "lastMessageTime" to System.currentTimeMillis()
        )
        chatsRef.child(chatId).updateChildren(updates)

        // Increment unread count for receiver
        val chatSnapshot = chatsRef.child(chatId).get()
        chatSnapshot.addOnSuccessListener { snapshot ->
            val user1Id = snapshot.child("user1Id").getValue(String::class.java) ?: ""
            val unreadField = if (receiverId == user1Id) "unreadCount_user1" else "unreadCount_user2"

            chatsRef.child(chatId).child(unreadField).runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentCount = currentData.getValue(Int::class.java) ?: 0
                    currentData.value = currentCount + 1
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        Log.e(TAG, "Error updating unread count: ${error.message}")
                    }
                }
            })
        }
    }

    /**
     * Mark chat as read
     */
    fun markChatAsRead(chatId: String, userId: String) {
        chatsRef.child(chatId).get().addOnSuccessListener { snapshot ->
            val user1Id = snapshot.child("user1Id").getValue(String::class.java) ?: ""
            val unreadField = if (userId == user1Id) "unreadCount_user1" else "unreadCount_user2"
            chatsRef.child(chatId).child(unreadField).setValue(0)
        }
    }
}

