package com.salmankhan.i221285.models

/**
 * Data class representing a Comment on a Post
 */
data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

