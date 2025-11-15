package com.salmankhan.i221285.models

/**
 * Data class representing a Follow Request
 */
data class FollowRequest(
    val fromUserId: String = "",
    val toUserId: String = "",
    val fromUsername: String = "",
    val fromUserProfileImage: String = "",
    val status: String = "pending", // "pending", "accepted", "rejected"
    val createdAt: Long = System.currentTimeMillis()
)

