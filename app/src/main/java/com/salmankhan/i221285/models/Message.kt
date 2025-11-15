package com.salmankhan.i221285.models

/**
 * Data class representing a Message in a Chat
 */
data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val type: String = "text", // "text", "image", "post"
    val content: String = "", // text or base64 image
    val postId: String = "", // if type is "post"
    val createdAt: Long = System.currentTimeMillis(),
    val editedAt: Long = 0,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false
) {
    /**
     * Check if message can be edited/deleted (within 5 minutes)
     */
    fun canEditOrDelete(): Boolean {
        val fiveMinutes = 5 * 60 * 1000L // 5 minutes in milliseconds
        return (System.currentTimeMillis() - createdAt) <= fiveMinutes
    }
}

