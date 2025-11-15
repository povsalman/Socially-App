package com.salmankhan.i221285.models

/**
 * Data class representing a User
 */
data class User(
    val uid: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val bio: String = "",
    val website: String = "",
    val email: String = "",
    val phone: String = "",
    val gender: String = "",
    val profileImage: String = "",
    val postsCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val profileCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getFullName(): String {
        return "$firstName $lastName".trim()
    }
}

