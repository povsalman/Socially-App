package com.salmankhan.i221285.models

/**
 * Data class representing a notification
 */
data class Notification(
    val id: String = "",
    val type: String = "", // "screenshot", "message", "follow_request", etc.
    val title: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
) {
    /**
     * Get time ago string
     */
    fun getTimeAgo(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60000 -> "Just now" // Less than 1 minute
            diff < 3600000 -> "${diff / 60000}m ago" // Less than 1 hour
            diff < 86400000 -> "${diff / 3600000}h ago" // Less than 1 day
            diff < 604800000 -> "${diff / 86400000}d ago" // Less than 1 week
            else -> "${diff / 604800000}w ago" // Weeks
        }
    }
}

