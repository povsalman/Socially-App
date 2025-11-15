package com.salmankhan.i221285.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.salmankhan.i221285.DmActivity
import com.salmankhan.i221285.HomeActivity
import com.salmankhan.i221285.R

class FCMService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "instagram_notifications"
        private const val CHANNEL_NAME = "Instagram Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        
        // Save token to user's Firebase profile
        val userId = com.salmankhan.i221285.AuthService.currentUser()?.uid
        if (userId != null) {
            val userRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
            userRef.child("fcmToken").setValue(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d(TAG, "Message received from: ${message.from}")
        
        // Handle notification payload
        message.notification?.let {
            showNotification(
                title = it.title ?: "New Notification",
                body = it.body ?: "",
                notificationType = message.data["type"] ?: "general"
            )
        }
        
        // Handle data payload
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${message.data}")
            handleDataPayload(message.data)
        }
    }

    private fun handleDataPayload(data: Map<String, String>) {
        val type = data["type"] ?: return
        val title = data["title"] ?: "New Notification"
        val body = data["body"] ?: ""
        
        showNotification(title, body, type)
    }

    private fun showNotification(title: String, body: String, notificationType: String) {
        createNotificationChannel()
        
        // Create intent based on notification type
        val intent = when (notificationType) {
            "message" -> Intent(this, DmActivity::class.java)
            "follow_request" -> {
                Intent(this, HomeActivity::class.java).apply {
                    putExtra("open_notifications", true)
                }
            }
            "screenshot" -> Intent(this, DmActivity::class.java)
            else -> Intent(this, HomeActivity::class.java)
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for messages, follow requests, and alerts"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

