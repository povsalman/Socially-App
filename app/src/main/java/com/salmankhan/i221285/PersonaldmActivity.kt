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
import com.salmankhan.i221285.adapters.MessagesAdapter
import com.salmankhan.i221285.models.Message
import com.salmankhan.i221285.services.MessageService
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    private val currentUserId: String? by lazy { AuthService.currentUser()?.uid }
    
    private var presenceListener: com.google.firebase.database.ValueEventListener? = null
    private var screenshotObserver: android.database.ContentObserver? = null
    private var callRequestListener: com.google.firebase.database.ValueEventListener? = null
    private var incomingCallDialog: androidx.appcompat.app.AlertDialog? = null
    private var lastScreenshotTime: Long = 0
    private val SCREENSHOT_COOLDOWN_MS = 5000L // 5 seconds cooldown to prevent duplicates

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
        
        // Setup incoming call listener
        setupIncomingCallListener()

        // Setup RecyclerView
        setupRecyclerView()

        // Load messages
        loadMessages()

        // Mark chat as read
        currentUserId?.let { userId ->
            chatId?.let { id ->
                MessageService.markChatAsRead(id, userId)
            }
        }

        // Video button click → Send call request
        val videoButton = findViewById<ImageView>(R.id.videocall_icon)
        videoButton.setOnClickListener {
            initiateCall()
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

        MessageService.getChatMessages(id) { messagesList ->
            runOnUiThread {
                messages.clear()
                messages.addAll(messagesList)
                messagesAdapter.notifyDataSetChanged()
                
                // Scroll to bottom
                if (messages.isNotEmpty()) {
                    messagesRecyclerView.scrollToPosition(messages.size - 1)
                }

                Log.d("PersonaldmActivity", "Loaded ${messagesList.size} messages")
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
            val result = MessageService.sendTextMessage(id, userId, otherId, text)
            if (result.isSuccess) {
                runOnUiThread {
                    messageInput.text.clear()
                }
            } else {
                runOnUiThread {
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
        val id = chatId ?: return

        lifecycleScope.launch {
            val result = MessageService.editMessage(id, message.id, newText)
            if (result.isSuccess) {
                runOnUiThread {
                    Toast.makeText(this@PersonaldmActivity, "Message edited", Toast.LENGTH_SHORT).show()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@PersonaldmActivity, result.exceptionOrNull()?.message ?: "Failed to edit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteMessage(message: Message) {
        val id = chatId ?: return

        lifecycleScope.launch {
            val result = MessageService.deleteMessage(id, message.id)
            if (result.isSuccess) {
                runOnUiThread {
                    Toast.makeText(this@PersonaldmActivity, "Message deleted", Toast.LENGTH_SHORT).show()
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
        
        lifecycleScope.launch {
            try {
                val userRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                
                val snapshot = userRef.get().await()
                val profileImageBase64 = snapshot.child("profileImage").getValue(String::class.java)
                
                runOnUiThread {
                    if (!profileImageBase64.isNullOrEmpty() && profileImageBase64 != "default") {
                        try {
                            val decodedBytes = android.util.Base64.decode(profileImageBase64, android.util.Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            profileImageView.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Log.e("PersonaldmActivity", "Error decoding profile image", e)
                            // Keep default placeholder
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
                val userRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUid)
                val snapshot = userRef.get().await()
                val username = snapshot.child("username").getValue(String::class.java) ?: "Someone"
                
                // Send screenshot notification
                com.salmankhan.i221285.services.NotificationHelper.sendScreenshotNotification(recipientId, username)
                
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
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up listeners
        otherUserId?.let { userId ->
            presenceListener?.let { listener ->
                com.salmankhan.i221285.services.PresenceService.stopListeningToUserStatus(userId, listener)
            }
        }
        
        screenshotObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        
        // Clean up call listener
        currentUserId?.let { userId ->
            callRequestListener?.let { listener ->
                com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("callRequests")
                    .child(userId)
                    .removeEventListener(listener)
            }
        }
        
        // Dismiss incoming call dialog if showing
        incomingCallDialog?.dismiss()
    }
    
    private fun initiateCall() {
        val currentUid = currentUserId ?: return
        val otherUid = otherUserId ?: return
        val otherName = otherUsername ?: "User"
        val chatIdValue = chatId ?: return
        
        lifecycleScope.launch {
            try {
                // Get current user's username
                val userRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUid)
                val snapshot = userRef.get().await()
                val currentUsername = snapshot.child("username").getValue(String::class.java) ?: "User"
                
                // Send call request
                val result = com.salmankhan.i221285.services.CallService.sendCallRequest(
                    callerId = currentUid,
                    callerUsername = currentUsername,
                    receiverId = otherUid,
                    chatId = chatIdValue
                )
                
                if (result.isSuccess) {
                    val callRequestId = result.getOrNull()!!
                    
                    // Start video call activity immediately for caller
                    runOnUiThread {
                        val intent = Intent(this@PersonaldmActivity, VideocallActivity::class.java)
                        intent.putExtra("other_user_id", otherUid)
                        intent.putExtra("other_username", otherName)
                        intent.putExtra("chat_id", chatIdValue)
                        intent.putExtra("call_request_id", callRequestId)
                        intent.putExtra("is_caller", true)
                        startActivity(intent)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@PersonaldmActivity,
                            "Failed to send call request",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("PersonaldmActivity", "Error initiating call: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(
                        this@PersonaldmActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun setupIncomingCallListener() {
        val userId = currentUserId ?: return
        
        val callRequestsRef = com.google.firebase.database.FirebaseDatabase.getInstance()
            .getReference("callRequests")
            .child(userId)
        
        callRequestListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                for (callSnapshot in snapshot.children) {
                    val status = callSnapshot.child("status").getValue(String::class.java)
                    val callerId = callSnapshot.child("callerId").getValue(String::class.java)
                    val callerUsername = callSnapshot.child("callerUsername").getValue(String::class.java)
                    val chatIdValue = callSnapshot.child("chatId").getValue(String::class.java)
                    val callRequestId = callSnapshot.child("id").getValue(String::class.java)
                    
                    // Only show dialog for pending calls
                    if (status == "pending" && callerId != null && callRequestId != null) {
                        // Check if this is a call from the current chat
                        if (chatIdValue == chatId) {
                            showIncomingCallDialog(
                                callerId = callerId,
                                callerUsername = callerUsername ?: "User",
                                callRequestId = callRequestId,
                                chatIdValue = chatIdValue ?: ""
                            )
                        }
                    }
                }
            }
            
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("PersonaldmActivity", "Error listening for calls: ${error.message}")
            }
        }
        
        callRequestsRef.addValueEventListener(callRequestListener!!)
    }
    
    private fun showIncomingCallDialog(
        callerId: String,
        callerUsername: String,
        callRequestId: String,
        chatIdValue: String
    ) {
        // Don't show if already showing
        if (incomingCallDialog?.isShowing == true) {
            return
        }
        
        runOnUiThread {
            val dialogView = layoutInflater.inflate(R.layout.dialog_incoming_call, null)
            val callerNameTextView = dialogView.findViewById<android.widget.TextView>(R.id.caller_name)
            val callerProfileImage = dialogView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.caller_profile_image)
            val acceptButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.accept_call_button)
            val declineButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.decline_call_button)
            
            callerNameTextView.text = callerUsername
            
            // Load caller's profile image
            lifecycleScope.launch {
                try {
                    val userRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(callerId)
                    val snapshot = userRef.get().await()
                    val profileImageBase64 = snapshot.child("profileImage").getValue(String::class.java)
                    
                    runOnUiThread {
                        if (!profileImageBase64.isNullOrEmpty()) {
                            val bitmap = StoryService.base64ToBitmap(profileImageBase64)
                            if (bitmap != null) {
                                callerProfileImage.setImageBitmap(bitmap)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PersonaldmActivity", "Error loading caller image: ${e.message}", e)
                }
            }
            
            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()
            
            acceptButton.setOnClickListener {
                acceptCall(callRequestId, callerId, callerUsername, chatIdValue)
                dialog.dismiss()
            }
            
            declineButton.setOnClickListener {
                declineCall(callRequestId)
                dialog.dismiss()
            }
            
            incomingCallDialog = dialog
            dialog.show()
            
            // Auto-dismiss after 30 seconds and mark as missed
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (dialog.isShowing) {
                    dialog.dismiss()
                    lifecycleScope.launch {
                        currentUserId?.let {
                            com.salmankhan.i221285.services.CallService.markCallAsMissed(it, callRequestId)
                        }
                    }
                }
            }, 30000)
        }
    }
    
    private fun acceptCall(callRequestId: String, callerId: String, callerUsername: String, chatIdValue: String) {
        val userId = currentUserId ?: return
        
        lifecycleScope.launch {
            try {
                // Accept the call request
                com.salmankhan.i221285.services.CallService.acceptCallRequest(userId, callRequestId)
                
                // Start video call activity
                runOnUiThread {
                    val intent = Intent(this@PersonaldmActivity, VideocallActivity::class.java)
                    intent.putExtra("other_user_id", callerId)
                    intent.putExtra("other_username", callerUsername)
                    intent.putExtra("chat_id", chatIdValue)
                    intent.putExtra("call_request_id", callRequestId)
                    intent.putExtra("is_caller", false)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e("PersonaldmActivity", "Error accepting call: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(
                        this@PersonaldmActivity,
                        "Error accepting call",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun declineCall(callRequestId: String) {
        val userId = currentUserId ?: return
        
        lifecycleScope.launch {
            try {
                com.salmankhan.i221285.services.CallService.declineCallRequest(userId, callRequestId)
                Toast.makeText(
                    this@PersonaldmActivity,
                    "Call declined",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e("PersonaldmActivity", "Error declining call: ${e.message}", e)
            }
        }
    }
}