package com.salmankhan.i221285.services

import android.util.Log
import com.google.firebase.database.*

object PresenceService {
    private const val TAG = "PresenceService"
    private val database = FirebaseDatabase.getInstance()
    private val presenceRef = database.getReference("presence")
    
    /**
     * Set user as online
     */
    fun setUserOnline(userId: String) {
        val userStatusRef = presenceRef.child(userId)
        
        // Set online status
        userStatusRef.setValue(mapOf(
            "status" to "online",
            "lastSeen" to System.currentTimeMillis()
        ))
        
        // Set up automatic offline on disconnect
        userStatusRef.onDisconnect().setValue(mapOf(
            "status" to "offline",
            "lastSeen" to ServerValue.TIMESTAMP
        ))
        
        Log.d(TAG, "User $userId set to online")
    }
    
    /**
     * Set user as offline
     */
    fun setUserOffline(userId: String) {
        presenceRef.child(userId).setValue(mapOf(
            "status" to "offline",
            "lastSeen" to System.currentTimeMillis()
        ))
        
        Log.d(TAG, "User $userId set to offline")
    }
    
    /**
     * Listen to user's online status
     */
    fun listenToUserStatus(userId: String, callback: (Boolean, Long?) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.child("status").getValue(String::class.java)
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java)
                
                val isOnline = status == "online"
                callback(isOnline, lastSeen)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error listening to user status: ${error.message}")
                callback(false, null)
            }
        }
        
        presenceRef.child(userId).addValueEventListener(listener)
        return listener
    }
    
    /**
     * Stop listening to user status
     */
    fun stopListeningToUserStatus(userId: String, listener: ValueEventListener) {
        presenceRef.child(userId).removeEventListener(listener)
    }
    
    /**
     * Format last seen time
     */
    fun formatLastSeen(lastSeenTimestamp: Long?): String {
        if (lastSeenTimestamp == null) return "Never"
        
        val now = System.currentTimeMillis()
        val diff = now - lastSeenTimestamp
        
        return when {
            diff < 60000 -> "Just now" // Less than 1 minute
            diff < 3600000 -> "${diff / 60000} minutes ago" // Less than 1 hour
            diff < 86400000 -> "${diff / 3600000} hours ago" // Less than 1 day
            diff < 604800000 -> "${diff / 86400000} days ago" // Less than 1 week
            else -> "${diff / 604800000} weeks ago"
        }
    }
}

