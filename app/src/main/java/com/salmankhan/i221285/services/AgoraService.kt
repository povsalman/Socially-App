package com.salmankhan.i221285.services

import android.util.Log
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import android.content.Context

/**
 * Service for managing Agora video calls
 * Note: Replace "YOUR_AGORA_APP_ID" with your actual Agora App ID from agora.io
 * 
 * To get your App ID:
 * 1. Go to https://console.agora.io/
 * 2. Sign up or log in
 * 3. Create a new project
 * 4. Copy the App ID from the project settings
 * 5. Replace "YOUR_AGORA_APP_ID" below with your actual App ID
 */
object AgoraService {
    private const val TAG = "AgoraService"
    private const val AGORA_APP_ID = "69b108ba7fbe4ab1973db038bc81f2b7" // TODO: Replace with your Agora App ID from agora.io console
    
    private var rtcEngine: RtcEngine? = null
    private var eventHandler: IRtcEngineEventHandler? = null
    
    /**
     * Initialize Agora RTC Engine
     */
    fun initialize(context: Context, handler: IRtcEngineEventHandler): Boolean {
        return try {
            // Check if App ID is set
            if (AGORA_APP_ID == "YOUR_AGORA_APP_ID" || AGORA_APP_ID.isEmpty()) {
                Log.e(TAG, "Agora App ID not configured! Please set your App ID in AgoraService.kt")
                return false
            }
            
            val config = RtcEngineConfig()
            config.mContext = context.applicationContext
            config.mAppId = AGORA_APP_ID
            config.mEventHandler = handler
            
            rtcEngine = RtcEngine.create(config)
            
            if (rtcEngine == null) {
                Log.e(TAG, "Failed to create RtcEngine - check your App ID")
                return false
            }
            
            eventHandler = handler
            
            // Enable audio first (required for calls)
            rtcEngine?.enableAudio()
            
            // Enable video
            rtcEngine?.enableVideo()
            
            // Set video encoder configuration
            rtcEngine?.setVideoEncoderConfiguration(
                io.agora.rtc2.video.VideoEncoderConfiguration(
                    io.agora.rtc2.video.VideoEncoderConfiguration.VD_640x360,
                    io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    io.agora.rtc2.video.VideoEncoderConfiguration.STANDARD_BITRATE,
                    io.agora.rtc2.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
                )
            )
            
            // Enable local audio and video
            rtcEngine?.muteLocalAudioStream(false)
            rtcEngine?.muteLocalVideoStream(false)
            rtcEngine?.setChannelProfile(io.agora.rtc2.Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine?.setClientRole(io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER)
            rtcEngine?.setDefaultAudioRoutetoSpeakerphone(true)
            
            // Set connection parameters for better stability
            try {
                rtcEngine?.setParameters("{\"rtc.log_filter\":65535}")
            } catch (e: Exception) {
                Log.w(TAG, "Could not set connection parameters: ${e.message}")
            }
            
            Log.d(TAG, "Agora RTC Engine initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Agora RTC Engine: ${e.message}", e)
            
            // Check for specific error codes
            val errorMessage = e.message ?: ""
            when {
                errorMessage.contains("error=101") || errorMessage.contains("101") -> {
                    Log.e(TAG, "Error 101: Invalid App ID. Please check your Agora App ID in AgoraService.kt")
                }
                errorMessage.contains("error=102") || errorMessage.contains("102") -> {
                    Log.e(TAG, "Error 102: Invalid channel name")
                }
                else -> {
                    Log.e(TAG, "Unknown error: $errorMessage")
                }
            }
            
            false
        }
    }
    
    /**
     * Join a channel
     * @param token Token for authentication (null for testing with App ID only)
     * @param channelName Channel name to join
     * @param uid User ID (0 for auto-generated)
     */
    fun joinChannel(token: String?, channelName: String, uid: Int): Int {
        return try {
            if (rtcEngine == null) {
                Log.e(TAG, "RTC Engine is null. Cannot join channel.")
                return -1
            }
            
            if (channelName.isEmpty()) {
                Log.e(TAG, "Channel name is empty")
                return -1
            }
            
            Log.d(TAG, "Attempting to join channel: $channelName with UID: $uid")
            
            // Use the token-based joinChannel method
            // For testing without token, pass empty string instead of null
            // Agora SDK requires a non-null String for token parameter
            val tokenToUse = token ?: ""
            val result = rtcEngine?.joinChannel(tokenToUse, channelName, "", uid) ?: -1
            
            if (result == 0) {
                Log.d(TAG, "Successfully initiated channel join")
            } else {
                Log.e(TAG, "Failed to join channel. Error code: $result")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error joining channel: ${e.message}", e)
            -1
        }
    }
    
    /**
     * Leave the channel
     */
    fun leaveChannel() {
        try {
            rtcEngine?.leaveChannel()
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving channel: ${e.message}", e)
        }
    }
    
    /**
     * Setup local video view
     */
    fun setupLocalVideo(surfaceView: android.view.SurfaceView) {
        try {
            rtcEngine?.setupLocalVideo(
                io.agora.rtc2.video.VideoCanvas(surfaceView)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up local video: ${e.message}", e)
        }
    }
    
    /**
     * Setup remote video view
     */
    fun setupRemoteVideo(uid: Int, surfaceView: android.view.SurfaceView) {
        try {
            rtcEngine?.setupRemoteVideo(
                io.agora.rtc2.video.VideoCanvas(surfaceView, io.agora.rtc2.Constants.RENDER_MODE_HIDDEN, uid)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up remote video: ${e.message}", e)
        }
    }
    
    /**
     * Enable/disable local video
     */
    fun enableLocalVideo(enabled: Boolean) {
        try {
            rtcEngine?.enableLocalVideo(enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling/disabling local video: ${e.message}", e)
        }
    }
    
    /**
     * Enable/disable local audio
     */
    fun muteLocalAudio(muted: Boolean) {
        try {
            rtcEngine?.muteLocalAudioStream(muted)
        } catch (e: Exception) {
            Log.e(TAG, "Error muting/unmuting local audio: ${e.message}", e)
        }
    }
    
    /**
     * Switch camera
     */
    fun switchCamera(): Boolean {
        return try {
            rtcEngine?.switchCamera() == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error switching camera: ${e.message}", e)
            false
        }
    }
    
    /**
     * Release Agora RTC Engine
     */
    fun release() {
        try {
            RtcEngine.destroy()
            rtcEngine = null
            eventHandler = null
            Log.d(TAG, "Agora RTC Engine released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing Agora RTC Engine: ${e.message}", e)
        }
    }
    
    /**
     * Get RTC Engine instance
     */
    fun getRtcEngine(): RtcEngine? = rtcEngine
}

