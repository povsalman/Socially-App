package com.salmankhan.i221285.services

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    
    suspend fun sendMessageNotification(recipientUserId: String, senderUsername: String, messageText: String) {
        try {
            // Get recipient's FCM token
            val userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(recipientUserId)
            
            val snapshot = userRef.get().await()
            val fcmToken = snapshot.child("fcmToken").getValue(String::class.java)
            
            if (fcmToken != null) {
                // Store notification in Firebase for in-app display
                val notificationRef = FirebaseDatabase.getInstance()
                    .getReference("notifications")
                    .child(recipientUserId)
                    .push()
                
                val notification = mapOf(
                    "type" to "message",
                    "title" to "New message from $senderUsername",
                    "body" to messageText,
                    "timestamp" to System.currentTimeMillis(),
                    "read" to false
                )
                
                notificationRef.setValue(notification).await()
                Log.d(TAG, "Message notification saved for user: $recipientUserId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message notification", e)
        }
    }
    
    suspend fun sendFollowRequestNotification(recipientUserId: String, senderUsername: String) {
        try {
            // Store notification in Firebase (always save, regardless of FCM token)
            val notificationRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(recipientUserId)
                .push()
            
            val notification = mapOf(
                "type" to "follow_request",
                "title" to "New follow request",
                "body" to "$senderUsername wants to follow you",
                "timestamp" to System.currentTimeMillis(),
                "read" to false
            )
            
            notificationRef.setValue(notification).await()
            Log.d(TAG, "Follow request notification saved for user: $recipientUserId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending follow request notification", e)
        }
    }
    
    suspend fun sendProfileVisitNotification(visitedUserId: String, visitorUsername: String) {
        try {
            // Store notification in Firebase (always save, regardless of FCM token)
            val notificationRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(visitedUserId)
                .push()
            
            val notification = mapOf(
                "type" to "profile_visit",
                "title" to "Profile visit",
                "body" to "$visitorUsername visited your profile",
                "timestamp" to System.currentTimeMillis(),
                "read" to false
            )
            
            notificationRef.setValue(notification).await()
            Log.d(TAG, "Profile visit notification saved for user: $visitedUserId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending profile visit notification", e)
        }
    }
    
    suspend fun sendScreenshotNotification(recipientUserId: String, screenshotterUsername: String) {
        try {
            // Store notification in Firebase (always save, regardless of FCM token)
            val notificationRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(recipientUserId)
                .push()
            
            val notification = mapOf(
                "type" to "screenshot",
                "title" to "Screenshot Alert",
                "body" to "$screenshotterUsername took a screenshot of your chat",
                "timestamp" to System.currentTimeMillis(),
                "read" to false
            )
            
            notificationRef.setValue(notification).await()
            Log.d(TAG, "Screenshot notification saved for user: $recipientUserId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending screenshot notification", e)
        }
    }
}

