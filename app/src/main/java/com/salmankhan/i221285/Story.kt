package com.salmankhan.i221285

import java.util.Date

/**
 * Data class representing a Story in Firebase Realtime Database
 * Stories are temporary and auto-delete after 24 hours
 */
data class Story(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val imageBase64: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // 24 hours from now
    val isActive: Boolean = true
) {
    /**
     * Check if story has expired (older than 24 hours)
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }
    
    /**
     * Get time remaining in hours
     */
    fun getTimeRemainingHours(): Long {
        val remaining = expiresAt - System.currentTimeMillis()
        return if (remaining > 0) remaining / (60 * 60 * 1000) else 0
    }
}
