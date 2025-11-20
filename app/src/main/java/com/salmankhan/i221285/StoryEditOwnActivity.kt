package com.salmankhan.i221285

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.salmankhan.i221285.SociallyApplication
import com.salmankhan.i221285.repository.UserRepository
import kotlinx.coroutines.launch

class StoryEditOwnActivity : AppCompatActivity() {
    
    private var capturedImageBase64: String? = null
    private var previewImageView: ImageView? = null
    private val userRepository by lazy { UserRepository(SociallyApplication.getInstance()) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_story_edit_own)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get the preview ImageView (assuming it exists in the layout)
        previewImageView = findViewById(R.id.story_preview_image)
        
        // Check if we have an image from camera
        val hasImage = intent.getBooleanExtra("hasImage", false)
        
        if (hasImage) {
            // Get the captured image from StoryTakeActivity's companion object
            capturedImageBase64 = StoryTakeActivity.tempCapturedImageBase64
            
            if (capturedImageBase64 != null) {
                // Display the captured image
                val bitmap = StoryService.base64ToBitmap(capturedImageBase64!!)
                if (bitmap != null && previewImageView != null) {
                    previewImageView?.setImageBitmap(bitmap)
                    Toast.makeText(this, "Image captured! Ready to post story.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error displaying captured image", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No image data found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show()
        }

        // Post arrow → Save story to Firebase and go to Home Screen
        val postIcon = findViewById<ImageView>(R.id.post_icon)
        postIcon.setOnClickListener {
            saveStory()
        }

        // Back arrow → Previous screen
        val backIcon = findViewById<ImageView>(R.id.close_icon)
        backIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun saveStory() {
        lifecycleScope.launch {
            try {
                // Get current user info
                val currentUser = AuthService.currentUser()
                if (currentUser == null) {
                    Toast.makeText(this@StoryEditOwnActivity, "Please login first", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Check if we have captured image
                if (capturedImageBase64 == null) {
                    Toast.makeText(this@StoryEditOwnActivity, "No image to upload", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                Toast.makeText(this@StoryEditOwnActivity, "Uploading story...", Toast.LENGTH_SHORT).show()
                
                val profileResult = userRepository.getUserProfile(currentUser.uid.toIntOrNull())
                val username = profileResult.getOrNull()?.username ?: currentUser.email ?: "User"
                val userProfileImage = profileResult.getOrNull()?.profileImage ?: ""
                
                val result = StoryService.uploadStory(
                    userId = currentUser.uid,
                    username = username,
                    userProfileImage = userProfileImage,
                    imageBase64 = capturedImageBase64!!
                )
                
                if (result.isSuccess) {
                    Toast.makeText(this@StoryEditOwnActivity, "Story posted successfully!", Toast.LENGTH_SHORT).show()
                    
                    // Clear temp data
                    StoryTakeActivity.tempCapturedImageBase64 = null
                    capturedImageBase64 = null
                    
                    // Navigate to Home Screen
                    val intent = Intent(this@StoryEditOwnActivity, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@StoryEditOwnActivity, "Failed to post story: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                Log.e("StoryEditOwnActivity", "Error saving story: ${e.message}", e)
                Toast.makeText(this@StoryEditOwnActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}