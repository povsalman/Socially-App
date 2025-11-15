package com.salmankhan.i221285.models

/**
 * Data class representing a Chat between two users
 */
data class Chat(
    val id: String = "", // combination of user IDs sorted
    val user1Id: String = "",
    val user2Id: String = "",
    val user1Name: String = "",
    val user2Name: String = "",
    val user1ProfileImage: String = "",
    val user2ProfileImage: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount_user1: Int = 0,
    val unreadCount_user2: Int = 0
) {
    companion object {
        /**
         * Generate chat ID from two user IDs
         */
        fun generateChatId(userId1: String, userId2: String): String {
            return if (userId1 < userId2) {
                "${userId1}_${userId2}"
            } else {
                "${userId2}_${userId1}"
            }
        }
    }
}

