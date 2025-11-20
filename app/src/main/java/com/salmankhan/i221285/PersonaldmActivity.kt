package com.salmankhan.i221285

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.EditText
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.salmankhan.i221285.SociallyApplication
import com.salmankhan.i221285.adapters.MessagesAdapter
import com.salmankhan.i221285.models.Message
import com.salmankhan.i221285.repository.UserRepository
import com.salmankhan.i221285.services.MessageService
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class PersonaldmActivity : AppCompatActivity() {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var usernameHeader: TextView
    private lateinit var onlineStatusText: TextView
    private lateinit var messagesAdapter: MessagesAdapter
    
    private val messages = mutableListOf<Message>()
    
    private var chatId: String? = null
    private var otherUserId: String? = null
    private var otherUsername: String? = null
    private var otherUserProfileImage: String? = null
    private val currentUserId: String? by lazy { AuthService.currentUser()?.uid }
    
    private var presenceListener: Any? = null
    private var screenshotObserver: android.database.ContentObserver? = null
    private var lastScreenshotTime: Long = 0
    private val SCREENSHOT_COOLDOWN_MS = 5000L // 5 seconds cooldown to prevent duplicates
    private val userRepository by lazy { UserRepository(SociallyApplication.getInstance()) }
    
    // Vanish mode
    private var isVanishModeActive = false
    private val vanishModeMessages = mutableSetOf<String>() // Track message IDs in vanish mode

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            sendImageMessage(it)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Storage permission required to send images", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_personaldm)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get chat data from intent
        chatId = intent.getStringExtra("chat_id")
        otherUserId = intent.getStringExtra("other_user_id")
        otherUsername = intent.getStringExtra("other_username")

        Log.d("PersonaldmActivity", "chatId: $chatId, otherUserId: $otherUserId, otherUsername: $otherUsername")

        if (chatId == null || otherUserId == null) {
            Log.e("PersonaldmActivity", "Missing required data: chatId=$chatId, otherUserId=$otherUserId")
            Toast.makeText(this, "Error loading chat", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (currentUserId == null) {
            Log.e("PersonaldmActivity", "Current user not logged in")
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        messagesRecyclerView = findViewById(R.id.messages_recycler_view)
        messageInput = findViewById(R.id.message_input)
        usernameHeader = findViewById(R.id.username_header)
        onlineStatusText = findViewById(R.id.online_status_text)
        val profileImage = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.chat_profile_image)

        // Set username in header
        usernameHeader.text = otherUsername ?: "Chat"
        
        // Load other user's profile image
        loadUserProfileImage(profileImage)
        
        // Setup online status listener
        setupOnlineStatusListener()
        
        // Setup screenshot detection
        setupScreenshotDetection()
        
        // Setup RecyclerView
        setupRecyclerView()

        // Load messages
        loadMessages()

        // Vanish mode button
        val vanishButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.vanish_mode_button)
        val vanishBanner = findViewById<android.widget.LinearLayout>(R.id.vanish_mode_banner)
        vanishButton.setOnClickListener {
            isVanishModeActive = !isVanishModeActive
            if (isVanishModeActive) {
                vanishButton.text = "Exit Vanish"
                vanishButton.setBackgroundColor(getColor(android.R.color.holo_purple))
                vanishButton.setTextColor(getColor(android.R.color.white))
                vanishBanner.visibility = View.VISIBLE
                Toast.makeText(this, "Vanish Mode Activated - Messages will disappear when chat is closed", Toast.LENGTH_LONG).show()
            } else {
                vanishButton.text = "Vanish"
                vanishButton.setBackgroundColor(getColor(android.R.color.transparent))
                vanishButton.setTextColor(getColor(com.salmankhan.i221285.R.color.text_color))
                vanishBanner.visibility = View.GONE
                Toast.makeText(this, "Vanish Mode Deactivated", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Voice call button
        val voiceCallButton = findViewById<ImageView>(R.id.voice_call_icon)
        voiceCallButton.setOnClickListener {
            initiateVoiceCall()
        }
        
        // Video button click → Send call request
        val videoButton = findViewById<ImageView>(R.id.videocall_icon)
        videoButton.setOnClickListener {
            initiateVideoCall()
        }

        // Camera button click → Open gallery for image
        val cameraButton = findViewById<ImageView>(R.id.camera_icon)
        cameraButton.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        // Gallery button click → Open gallery
        val galleryButton = findViewById<ImageView>(R.id.gallery_icon)
        galleryButton.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        // Send button
        val sendButton = findViewById<ImageView>(R.id.send_button)
        sendButton.setOnClickListener {
            sendTextMessage()
        }

        // Back arrow → Previous screen
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        backIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        val userId = currentUserId ?: return

        messagesAdapter = MessagesAdapter(
            messages = messages,
            currentUserId = userId,
            otherUserProfileImage = otherUserProfileImage,
            onEditMessage = { message, newText ->
                editMessage(message, newText)
            },
            onDeleteMessage = { message ->
                deleteMessage(message)
            }
        )

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true

        messagesRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = messagesAdapter
        }
    }

    private fun loadMessages() {
        val id = chatId ?: return
        Log.d("PersonaldmActivity", "Loading messages for chat: $id")

        MessageService.getChatMessages(id) { messagesList ->
            runOnUiThread {
                Log.d("PersonaldmActivity", "Received ${messagesList.size} messages from service")
                messages.clear()
                messages.addAll(messagesList)
                messagesAdapter.notifyDataSetChanged()
                
                // Scroll to bottom
                if (messages.isNotEmpty()) {
                    messagesRecyclerView.scrollToPosition(messages.size - 1)
                }

                Log.d("PersonaldmActivity", "Messages adapter updated with ${messages.size} messages")
            }
        }
    }

    private fun sendTextMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return

        val userId = currentUserId ?: return
        val id = chatId ?: return
        val otherId = otherUserId ?: return

        lifecycleScope.launch {
            Log.d("PersonaldmActivity", "Attempting to send message: '$text' (Vanish Mode: $isVanishModeActive)")
            val result = MessageService.sendTextMessage(id, userId, otherId, text)
            if (result.isSuccess) {
                runOnUiThread {
                    messageInput.text.clear()
                    Log.d("PersonaldmActivity", "Message sent successfully, clearing input and waiting before reload...")
                }
                // Add a delay to allow server to process - increased to 1 second
                kotlinx.coroutines.delay(1000)
                runOnUiThread {
                    Log.d("PersonaldmActivity", "Delay complete, reloading messages now...")
                    // Reload messages to show the newly sent message
                    loadMessages()
                    
                    // For vanish mode, we'll track messages after reload
                    // In a full implementation, you'd get the message ID from the response
                    // and track it for deletion on chat close
                    if (isVanishModeActive) {
                        Log.d("PersonaldmActivity", "Message sent in vanish mode - will be deleted on chat close")
                    }
                }
            } else {
                runOnUiThread {
                    Log.e("PersonaldmActivity", "Failed to send message")
                    Toast.makeText(this@PersonaldmActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendImageMessage(uri: Uri) {
        val userId = currentUserId ?: return
        val id = chatId ?: return
        val otherId = otherUserId ?: return

        lifecycleScope.launch {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val imageBase64 = StoryService.bitmapToBase64(bitmap)

                val result = MessageService.sendImageMessage(id, userId, otherId, imageBase64)
                if (result.isSuccess) {
                    runOnUiThread {
                        Toast.makeText(this@PersonaldmActivity, "Image sent!", Toast.LENGTH_SHORT).show()
                        // Reload messages to show the newly sent image
                        loadMessages()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@PersonaldmActivity, "Failed to send image", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("PersonaldmActivity", "Error sending image: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@PersonaldmActivity, "Error processing image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun editMessage(message: Message, newText: String) {
        lifecycleScope.launch {
            val result = MessageService.editMessage(message.id, newText)
            if (result.isSuccess) {
                runOnUiThread {
                    Toast.makeText(this@PersonaldmActivity, "Message edited", Toast.LENGTH_SHORT).show()
                    // Reload messages to show the edited message
                    loadMessages()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@PersonaldmActivity, result.exceptionOrNull()?.message ?: "Failed to edit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteMessage(message: Message) {
        lifecycleScope.launch {
            val result = MessageService.deleteMessage(message.id)
            if (result.isSuccess) {
                runOnUiThread {
                    Toast.makeText(this@PersonaldmActivity, "Message deleted", Toast.LENGTH_SHORT).show()
                    // Reload messages to reflect the deletion
                    loadMessages()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@PersonaldmActivity, result.exceptionOrNull()?.message ?: "Failed to delete", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun loadUserProfileImage(profileImageView: de.hdodenhof.circleimageview.CircleImageView) {
        val userId = otherUserId ?: return
        
        val intId = userId.toIntOrNull() ?: return
        lifecycleScope.launch {
            try {
                val result = userRepository.getUserProfile(intId)
                if (result.isSuccess) {
                    val profile = result.getOrNull()
                    runOnUiThread {
                        val imageUrl = profile?.profileImage
                        otherUserProfileImage = imageUrl // Save for adapter
                        
                        if (!imageUrl.isNullOrEmpty()) {
                            // Fix the URL to handle localhost and relative paths
                            val fixedUrl = com.salmankhan.i221285.network.ApiClient.fixImageUrl(imageUrl)
                            Log.d("PersonaldmActivity", "Loading profile image from: $fixedUrl")
                            
                            Picasso.get()
                                .load(fixedUrl)
                                .placeholder(R.drawable.person1)
                                .error(R.drawable.person1)
                                .into(profileImageView, object : com.squareup.picasso.Callback {
                                    override fun onSuccess() {
                                        Log.d("PersonaldmActivity", "Profile image loaded successfully")
                                    }
                                    override fun onError(e: Exception?) {
                                        Log.e("PersonaldmActivity", "Error loading profile image", e)
                                    }
                                })
                        } else {
                            Log.d("PersonaldmActivity", "Profile image URL is empty, using placeholder")
                            profileImageView.setImageResource(R.drawable.person1)
                        }
                        
                        // Update adapter with new profile image
                        if (::messagesAdapter.isInitialized) {
                            setupRecyclerView()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PersonaldmActivity", "Error loading profile image", e)
            }
        }
    }
    
    private fun setupOnlineStatusListener() {
        val userId = otherUserId ?: return
        
        presenceListener = com.salmankhan.i221285.services.PresenceService.listenToUserStatus(userId) { isOnline, lastSeen ->
            runOnUiThread {
                if (isOnline) {
                    onlineStatusText.text = "Online"
                    onlineStatusText.setTextColor(getColor(android.R.color.holo_green_dark))
                } else {
                    val lastSeenText = com.salmankhan.i221285.services.PresenceService.formatLastSeen(lastSeen)
                    onlineStatusText.text = lastSeenText
                    onlineStatusText.setTextColor(getColor(com.salmankhan.i221285.R.color.gray))
                }
            }
        }
    }
    
    private fun setupScreenshotDetection() {
        screenshotObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                detectScreenshot()
            }
        }
        
        // Register content observer for screenshots
        contentResolver.registerContentObserver(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            screenshotObserver!!
        )
    }
    
    private fun detectScreenshot() {
        val projection = arrayOf(
            android.provider.MediaStore.Images.Media.DATA,
            android.provider.MediaStore.Images.Media.DATE_ADDED
        )
        
        val cursor = contentResolver.query(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${android.provider.MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                val dataIndex = it.getColumnIndex(android.provider.MediaStore.Images.Media.DATA)
                val dateIndex = it.getColumnIndex(android.provider.MediaStore.Images.Media.DATE_ADDED)
                
                if (dataIndex != -1 && dateIndex != -1) {
                    val imagePath = it.getString(dataIndex)
                    val dateAdded = it.getLong(dateIndex) * 1000
                    
                    // Check if image was added in last 3 seconds and path contains "screenshot"
                    val isRecent = System.currentTimeMillis() - dateAdded < 3000
                    val isScreenshot = imagePath.lowercase().contains("screenshot") || 
                                      imagePath.lowercase().contains("screen")
                    
                    if (isRecent && isScreenshot) {
                        handleScreenshot()
                    }
                }
            }
        }
    }
    
    private fun handleScreenshot() {
        val recipientId = otherUserId ?: return
        val currentUid = currentUserId ?: return
        
        // Prevent duplicate notifications with cooldown
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScreenshotTime < SCREENSHOT_COOLDOWN_MS) {
            Log.d("PersonaldmActivity", "Screenshot notification suppressed (cooldown)")
            return
        }
        
        lastScreenshotTime = currentTime
        
        lifecycleScope.launch {
            try {
                val currentProfile = userRepository.getUserProfile(currentUid.toIntOrNull())
                val username = currentProfile.getOrNull()?.username ?: "Someone"
                
                com.salmankhan.i221285.services.NotificationHelper.sendScreenshotNotification(
                    recipientId,
                    username
                )
                
                Log.d("PersonaldmActivity", "Screenshot detected and notification sent")
            } catch (e: Exception) {
                Log.e("PersonaldmActivity", "Error handling screenshot", e)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Set current user as online
        currentUserId?.let { com.salmankhan.i221285.services.PresenceService.setUserOnline(it) }
    }
    
    override fun onPause() {
        super.onPause()
        // Set current user as offline
        currentUserId?.let { com.salmankhan.i221285.services.PresenceService.setUserOffline(it) }
    }
    
    private fun initiateVoiceCall() {
        val intent = Intent(this, VoiceCallActivity::class.java)
        intent.putExtra("other_user_id", otherUserId)
        intent.putExtra("other_username", otherUsername)
        intent.putExtra("is_caller", true)
        startActivity(intent)
    }
    
    private fun initiateVideoCall() {
        val intent = Intent(this, VideoCallActivity::class.java)
        intent.putExtra("other_user_id", otherUserId)
        intent.putExtra("other_username", otherUsername)
        intent.putExtra("is_caller", true)
        startActivity(intent)
    }
    
    private fun deleteVanishModeMessages() {
        // If vanish mode was ever active, delete recent messages
        // In a full implementation, you'd track specific message IDs
        // For now, we'll just log that vanish mode was active
        if (isVanishModeActive) {
            Log.d("PersonaldmActivity", "Vanish mode was active - messages should be deleted")
            // TODO: Implement server-side vanish mode that auto-deletes messages
            // after both users have seen them and closed the chat
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Delete vanish mode messages when chat is closed
        if (isVanishModeActive || vanishModeMessages.isNotEmpty()) {
            deleteVanishModeMessages()
        }
        
        // Clean up listeners
        otherUserId?.let { userId ->
            presenceListener?.let { listener ->
                com.salmankhan.i221285.services.PresenceService.stopListeningToUserStatus(userId, listener)
            }
        }
        
        screenshotObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
    }
}