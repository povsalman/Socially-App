package com.salmankhan.i221285

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class VideoCallActivity : AppCompatActivity() {
    
    // Agora Configuration
    private val APP_ID = "YOUR_AGORA_APP_ID" // Replace with your Agora App ID
    private var mRtcEngine: RtcEngine? = null
    private var channelName: String = ""
    
    private var otherUserId: String? = null
    private var otherUsername: String? = null
    private var isCaller: Boolean = false
    private var isMuted = false
    private var isVideoOn = true
    
    private lateinit var localVideoContainer: FrameLayout
    private lateinit var remoteVideoContainer: FrameLayout
    private lateinit var usernameText: TextView
    private lateinit var statusText: TextView
    private lateinit var muteButton: ImageView
    private lateinit var videoButton: ImageView
    private lateinit var switchCameraButton: ImageView
    private lateinit var endCallButton: ImageView
    
    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)
        
        // Get data from intent
        otherUserId = intent.getStringExtra("other_user_id")
        otherUsername = intent.getStringExtra("other_username")
        isCaller = intent.getBooleanExtra("is_caller", false)
        
        // Initialize views
        localVideoContainer = findViewById(R.id.local_video_container)
        remoteVideoContainer = findViewById(R.id.remote_video_container)
        usernameText = findViewById(R.id.call_username)
        statusText = findViewById(R.id.call_status)
        muteButton = findViewById(R.id.mute_button)
        videoButton = findViewById(R.id.video_button)
        switchCameraButton = findViewById(R.id.switch_camera_button)
        endCallButton = findViewById(R.id.end_call_button)
        
        usernameText.text = otherUsername ?: "Unknown"
        statusText.text = if (isCaller) "Calling..." else "Incoming call..."
        
        // Check permissions
        if (checkSelfPermission()) {
            initializeAgoraEngine()
        }
        
        // Setup button listeners
        muteButton.setOnClickListener {
            toggleMute()
        }
        
        videoButton.setOnClickListener {
            toggleVideo()
        }
        
        switchCameraButton.setOnClickListener {
            switchCamera()
        }
        
        endCallButton.setOnClickListener {
            endCall()
        }
    }
    
    private fun checkSelfPermission(): Boolean {
        val audioGranted = ContextCompat.checkSelfPermission(
            this,
            REQUESTED_PERMISSIONS[0]
        ) == PackageManager.PERMISSION_GRANTED
        
        val cameraGranted = ContextCompat.checkSelfPermission(
            this,
            REQUESTED_PERMISSIONS[1]
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!audioGranted || !cameraGranted) {
            ActivityCompat.requestPermissions(
                this,
                REQUESTED_PERMISSIONS,
                PERMISSION_REQ_ID
            )
            return false
        }
        return true
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.isNotEmpty() && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                initializeAgoraEngine()
            } else {
                Toast.makeText(this, "Audio and camera permissions required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun initializeAgoraEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = applicationContext
            config.mAppId = APP_ID
            config.mEventHandler = mRtcEventHandler
            
            mRtcEngine = RtcEngine.create(config)
            mRtcEngine?.enableVideo()
            
            // Setup local video
            setupLocalVideo()
            
            // Generate channel name based on user IDs
            channelName = generateChannelName()
            
            // Join channel
            joinChannel()
            
        } catch (e: Exception) {
            Log.e("VideoCallActivity", "Error initializing Agora: ${e.message}", e)
            Toast.makeText(this, "Failed to initialize call engine", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupLocalVideo() {
        val surfaceView = SurfaceView(baseContext)
        surfaceView.setZOrderMediaOverlay(true)
        localVideoContainer.addView(surfaceView)
        
        mRtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        mRtcEngine?.startPreview()
    }
    
    private fun setupRemoteVideo(uid: Int) {
        val surfaceView = SurfaceView(baseContext)
        remoteVideoContainer.addView(surfaceView)
        
        mRtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
    }
    
    private fun generateChannelName(): String {
        val currentUserId = AuthService.getCurrentUserId() ?: "0"
        val ids = listOf(currentUserId, otherUserId ?: "0").sorted()
        return "video_${ids[0]}_${ids[1]}"
    }
    
    private fun joinChannel() {
        // In production, you should fetch token from your server
        mRtcEngine?.joinChannel(null, channelName, "", 0)
        statusText.text = "Connecting..."
    }
    
    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                statusText.text = "Connected"
                Log.d("VideoCallActivity", "Joined channel: $channel with uid: $uid")
            }
        }
        
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                statusText.text = "In call"
                setupRemoteVideo(uid)
                Log.d("VideoCallActivity", "Remote user joined: $uid")
            }
        }
        
        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                statusText.text = "Call ended"
                remoteVideoContainer.removeAllViews()
                Toast.makeText(this@VideoCallActivity, "Call ended", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        
        override fun onError(err: Int) {
            runOnUiThread {
                Log.e("VideoCallActivity", "Agora error: $err")
                Toast.makeText(this@VideoCallActivity, "Call error: $err", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun toggleMute() {
        isMuted = !isMuted
        mRtcEngine?.muteLocalAudioStream(isMuted)
        muteButton.setImageResource(if (isMuted) R.drawable.voicerec else R.drawable.voicerec)
        muteButton.alpha = if (isMuted) 0.5f else 1.0f
    }
    
    private fun toggleVideo() {
        isVideoOn = !isVideoOn
        mRtcEngine?.muteLocalVideoStream(!isVideoOn)
        videoButton.setImageResource(if (isVideoOn) R.drawable.video else R.drawable.video)
        videoButton.alpha = if (isVideoOn) 1.0f else 0.5f
        localVideoContainer.visibility = if (isVideoOn) View.VISIBLE else View.GONE
    }
    
    private fun switchCamera() {
        mRtcEngine?.switchCamera()
    }
    
    private fun endCall() {
        mRtcEngine?.leaveChannel()
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mRtcEngine?.leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null
    }
}
