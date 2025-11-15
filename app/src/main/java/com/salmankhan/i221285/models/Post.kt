package com.salmankhan.i221285.models

/**
 * Data class representing a Post in Firebase Realtime Database
 */
data class Post(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val imageBase64: String = "",
    val caption: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0
)

