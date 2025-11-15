package com.salmankhan.i221285

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.salmankhan.i221285.services.AgoraService
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import kotlinx.coroutines.launch
import java.util.*

class VideocallActivity : AppCompatActivity() {
    
    private var otherUserId: String? = null
    private var otherUsername: String? = null
    private var chatId: String? = null
    private val currentUserId: String? by lazy { AuthService.currentUser()?.uid }
    
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null
    private var isMuted = false
    private var isVideoEnabled = true
    private var callStartTime: Long = 0
    private var callDurationTimer: Timer? = null
    
    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.d("VideocallActivity", "Successfully joined channel: $channel with UID: $uid")
                callStartTime = System.currentTimeMillis()
                startCallDurationTimer()
            }
        }
        
        override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.d("VideocallActivity", "Rejoined channel: $channel")
            }
        }
        
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.d("VideocallActivity", "Remote user joined: $uid")
                setupRemoteVideo(uid)
            }
        }
        
        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Log.d("VideocallActivity", "Remote user left: $uid, reason: $reason")
                remoteSurfaceView?.visibility = View.GONE
                Toast.makeText(this@VideocallActivity, "User left the call", Toast.LENGTH_SHORT).show()
            }
        }
        
        override fun onError(err: Int) {
            runOnUiThread {
                val errorMessage = when (err) {
                    101 -> "Invalid App ID"
                    102 -> "Invalid channel name"
                    103 -> "Join channel rejected"
                    104 -> "Leave channel rejected"
                    105 -> "Already in use"
                    106 -> "Invalid token"
                    107 -> "Token expired"
                    108 -> "Invalid user ID"
                    109 -> "Connection interrupted"
                    110 -> "Connection lost. Check your network"
                    111 -> "Connection timeout"
                    112 -> "Connection rejected"
                    else -> "Error code: $err"
                }
                Log.e("VideocallActivity", "Agora error: $err - $errorMessage")
                Toast.makeText(this@VideocallActivity, "Call error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
        
        override fun onConnectionStateChanged(state: Int, reason: Int) {
            super.onConnectionStateChanged(state, reason)
            val stateName = when (state) {
                io.agora.rtc2.Constants.CONNECTION_STATE_DISCONNECTED -> "Disconnected"
                io.agora.rtc2.Constants.CONNECTION_STATE_CONNECTING -> "Connecting"
                io.agora.rtc2.Constants.CONNECTION_STATE_CONNECTED -> "Connected"
                io.agora.rtc2.Constants.CONNECTION_STATE_RECONNECTING -> "Reconnecting"
                io.agora.rtc2.Constants.CONNECTION_STATE_FAILED -> "Failed"
                else -> "Unknown ($state)"
            }
            
            val reasonName = when (reason) {
                io.agora.rtc2.Constants.CONNECTION_CHANGED_CONNECTING -> "Connecting"
                io.agora.rtc2.Constants.CONNECTION_CHANGED_JOIN_SUCCESS -> "Join success"
                io.agora.rtc2.Constants.CONNECTION_CHANGED_INTERRUPTED -> "Interrupted"
                io.agora.rtc2.Constants.CONNECTION_CHANGED_BANNED_BY_SERVER -> "Banned by server"
                io.agora.rtc2.Constants.CONNECTION_CHANGED_JOIN_FAILED -> "Join failed"
                io.agora.rtc2.Constants.CONNECTION_CHANGED_LEAVE_CHANNEL -> "Leave channel"
                io.agora.rtc2.Constants.CONNECTION_CHANGED_INVALID_APP_ID -> "Invalid App ID"
                io.agora.rtc2.Constants.CONNECTION_CHANGED_INVALID_CHANNEL_NAME -> "Invalid channel name"
                io.agora.rtc2.Constants.CONNECTION_CHANGED_INVALID_TOKEN -> "Invalid token"
                io.agora.rtc2.Constants.CONNECTION_CHANGED_TOKEN_EXPIRED -> "Token expired"
                io.agora.rtc2.Constants.CONNECTION_CHANGED_REJECTED_BY_SERVER -> "Rejected by server"
                io.agora.rtc2.Constants.CONNECTION_CHANGED_SETTING_PROXY_SERVER -> "Setting proxy"
                io.agora.rtc2.Constants.CONNECTION_CHANGED_RENEW_TOKEN -> "Renew token"
                io.agora.rtc2.Constants.CONNECTION_CHANGED_CLIENT_IP_ADDRESS_CHANGED -> "IP changed"
                io.agora.rtc2.Constants.CONNECTION_CHANGED_KEEP_ALIVE_TIMEOUT -> "Keep alive timeout"
                else -> "Unknown ($reason)"
            }
            
            val message = "Connection: $stateName (reason: $reasonName)"
            Log.d("VideocallActivity", message)
            
            runOnUiThread {
                when (state) {
                    io.agora.rtc2.Constants.CONNECTION_STATE_FAILED -> {
                        // Try to rejoin on failure
                        if (reason == io.agora.rtc2.Constants.CONNECTION_CHANGED_INVALID_APP_ID ||
                            reason == io.agora.rtc2.Constants.CONNECTION_CHANGED_INVALID_CHANNEL_NAME ||
                            reason == io.agora.rtc2.Constants.CONNECTION_CHANGED_INVALID_TOKEN) {
                            Toast.makeText(
                                this@VideocallActivity, 
                                "Connection failed: $reasonName. Please check your configuration.", 
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            // Network-related issues - try to rejoin
                            Toast.makeText(
                                this@VideocallActivity, 
                                "Connection lost. Attempting to reconnect...", 
                                Toast.LENGTH_SHORT
                            ).show()
                            // Attempt to rejoin
                            lifecycleScope.launch {
                                val channelName = generateChannelName()
                                val uid = currentUserId?.hashCode()?.and(0x7FFFFFFF) ?: 0
                                AgoraService.joinChannel("", channelName, uid)
                            }
                        }
                    }
                    io.agora.rtc2.Constants.CONNECTION_STATE_RECONNECTING -> {
                        Toast.makeText(
                            this@VideocallActivity, 
                            "Reconnecting...", 
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    io.agora.rtc2.Constants.CONNECTION_STATE_CONNECTED -> {
                        // Connection restored
                        Log.d("VideocallActivity", "Connection restored successfully")
                    }
                    io.agora.rtc2.Constants.CONNECTION_STATE_DISCONNECTED -> {
                        Toast.makeText(
                            this@VideocallActivity, 
                            "Disconnected from call", 
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check each permission individually
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        
        if (cameraGranted && audioGranted) {
            // All permissions granted, proceed with call
            initializeAgoraAndJoinCall()
        } else {
            // Some permissions denied
            val missingPermissions = mutableListOf<String>()
            if (!cameraGranted) missingPermissions.add("Camera")
            if (!audioGranted) missingPermissions.add("Microphone")
            
            Toast.makeText(
                this, 
                "Please grant ${missingPermissions.joinToString(" and ")} permissions in Settings", 
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_videocall)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get intent data
        otherUserId = intent.getStringExtra("other_user_id")
        otherUsername = intent.getStringExtra("other_username")
        chatId = intent.getStringExtra("chat_id")
        
        // Update UI with username
        val usernameText = findViewById<TextView>(R.id.username_text)
        usernameText?.text = otherUsername ?: "User"
        
        // Request permissions
        requestPermissions()
        
        // End call button
        val endcallIcon = findViewById<ImageView>(R.id.endcall_icon)
        endcallIcon?.setOnClickListener {
            endCall()
        }

        // Back arrow
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        backIcon?.setOnClickListener {
            endCall()
        }
        
        // Mute/unmute audio button
        val speakerIcon = findViewById<ImageView>(R.id.speaker_icon)
        speakerIcon?.setOnClickListener {
            toggleMute()
        }
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        // Check each permission individually
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        
        if (cameraGranted && audioGranted) {
            // All permissions already granted
            initializeAgoraAndJoinCall()
        } else {
            // Request missing permissions
            permissionLauncher.launch(permissions)
        }
    }
    
    private fun initializeAgoraAndJoinCall() {
        lifecycleScope.launch {
            try {
                // Initialize Agora
                val initialized = AgoraService.initialize(this@VideocallActivity, rtcEventHandler)
                if (!initialized) {
                    runOnUiThread {
                        Toast.makeText(
                            this@VideocallActivity, 
                            "Failed to initialize video call. Please check Agora App ID configuration.", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    finish()
                    return@launch
                }
                
                // Generate channel name from chat ID or user IDs
                val channelName = generateChannelName()
                val uid = currentUserId?.hashCode()?.and(0x7FFFFFFF) ?: 0
                
                Log.d("VideocallActivity", "Joining channel: $channelName with UID: $uid")
                
                // Join channel (empty string token for testing with App ID only)
                // In production, you should generate a token from your server
                // Pass empty string instead of null to avoid token validation errors
                val result = AgoraService.joinChannel("", channelName, uid)
                if (result != 0) {
                    runOnUiThread {
                        Toast.makeText(
                            this@VideocallActivity, 
                            "Failed to join call (Error: $result). Check logs for details.", 
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("VideocallActivity", "Failed to join channel. Error code: $result")
                    }
                    finish()
                    return@launch
                }
                
                // Setup local video after joining channel
                runOnUiThread {
                    setupLocalVideo()
                }
            } catch (e: Exception) {
                Log.e("VideocallActivity", "Error initializing call: ${e.message}", e)
                Toast.makeText(this@VideocallActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun generateChannelName(): String {
        // Use chat ID if available, otherwise combine user IDs
        // Ensure channel name is not too long (Agora limit is 64 characters)
        val baseName = chatId ?: listOfNotNull(currentUserId, otherUserId).sorted().joinToString("_")
        
        // Truncate if too long (Agora channel name max length is 64)
        return if (baseName.length > 64) {
            baseName.substring(0, 64)
        } else {
            baseName
        }
    }
    
    private fun setupLocalVideo() {
        try {
            // Create local video view using SurfaceView
            localSurfaceView = SurfaceView(this)
            localSurfaceView?.setZOrderMediaOverlay(true)
            
            // Add to layout dynamically
            val videoContainer = findViewById<android.widget.FrameLayout>(R.id.local_video_container)
            if (videoContainer != null && localSurfaceView != null) {
                videoContainer.addView(localSurfaceView)
                AgoraService.setupLocalVideo(localSurfaceView!!)
                Log.d("VideocallActivity", "Local video setup completed")
            } else {
                Log.e("VideocallActivity", "Video container or surface view is null")
            }
        } catch (e: Exception) {
            Log.e("VideocallActivity", "Error setting up local video: ${e.message}", e)
        }
    }
    
    private fun setupRemoteVideo(uid: Int) {
        try {
            // Create remote video view using SurfaceView
            remoteSurfaceView = SurfaceView(this)
            remoteSurfaceView?.setZOrderMediaOverlay(true)
            
            // Add to layout dynamically
            val videoContainer = findViewById<android.widget.FrameLayout>(R.id.remote_video_container)
            videoContainer?.addView(remoteSurfaceView)
            
            AgoraService.setupRemoteVideo(uid, remoteSurfaceView!!)
            remoteSurfaceView?.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e("VideocallActivity", "Error setting up remote video: ${e.message}", e)
        }
    }
    
    private fun toggleMute() {
        isMuted = !isMuted
        AgoraService.muteLocalAudio(isMuted)
        
        val speakerIcon = findViewById<ImageView>(R.id.speaker_icon)
        // You can change the icon based on mute state
        Toast.makeText(this, if (isMuted) "Muted" else "Unmuted", Toast.LENGTH_SHORT).show()
    }
    
    private fun startCallDurationTimer() {
        callDurationTimer = Timer()
        val durationText = findViewById<TextView>(R.id.call_duration_text)
        
        callDurationTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    val duration = System.currentTimeMillis() - callStartTime
                    val seconds = (duration / 1000) % 60
                    val minutes = (duration / 1000) / 60
                    durationText?.text = String.format("%02d:%02d", minutes, seconds)
                }
            }
        }, 0, 1000)
    }
    
    private fun endCall() {
        callDurationTimer?.cancel()
        AgoraService.leaveChannel()
        AgoraService.release()
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        endCall()
    }
    
    override fun onBackPressed() {
        endCall()
    }
}