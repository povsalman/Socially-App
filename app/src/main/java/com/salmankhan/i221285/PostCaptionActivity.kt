package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.FirebaseDatabase
import com.salmankhan.i221285.AuthService
import com.salmankhan.i221285.services.PostService
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PostCaptionActivity : AppCompatActivity() {

    private var captionEditText: EditText? = null
    private var previewImageView: ImageView? = null
    private var postButton: TextView? = null
    private var backButton: ImageView? = null

    companion object {
        // Temporary storage for selected post image
        var tempPostImageBase64: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_caption)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find views
        captionEditText = findViewById(R.id.caption_edit_text)
        previewImageView = findViewById(R.id.post_preview_image_small)
        postButton = findViewById(R.id.post_button)
        backButton = findViewById(R.id.back_arrow)

        // Load image from temporary storage
        val imageBase64 = PostImageFragment.tempSelectedImageBase64
        if (imageBase64 != null) {
            val bitmap = StoryService.base64ToBitmap(imageBase64)
            previewImageView?.setImageBitmap(bitmap)
        } else {
            Toast.makeText(this, "No image found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Back button
        backButton?.setOnClickListener {
            finish()
        }

        // Post button
        postButton?.setOnClickListener {
            uploadPost()
        }
    }

    private fun uploadPost() {
        lifecycleScope.launch {
            try {
                val currentUser = AuthService.currentUser()
                if (currentUser == null) {
                    Toast.makeText(this@PostCaptionActivity, "Please login first", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val imageBase64 = PostImageFragment.tempSelectedImageBase64
                if (imageBase64 == null) {
                    Toast.makeText(this@PostCaptionActivity, "No image to upload", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val caption = captionEditText?.text?.toString()?.trim() ?: ""

                Toast.makeText(this@PostCaptionActivity, "Uploading post...", Toast.LENGTH_SHORT).show()

                // Get user profile info from Firebase
                val userRef = FirebaseDatabase.getInstance()
                    .getReference("users").child(currentUser.uid)

                try {
                    val snapshot = userRef.get().await()
                    val username = snapshot.child("username").getValue(String::class.java) ?: currentUser.email ?: "User"
                    val userProfileImage = snapshot.child("profileImage").getValue(String::class.java) ?: ""

                    // Upload post to Firebase
                    val result = PostService.uploadPost(
                        userId = currentUser.uid,
                        username = username,
                        userProfileImage = userProfileImage,
                        imageBase64 = imageBase64,
                        caption = caption
                    )

                    if (result.isSuccess) {
                        Toast.makeText(this@PostCaptionActivity, "Post uploaded successfully!", Toast.LENGTH_SHORT).show()

                        // Clear temp data
                        PostImageFragment.tempSelectedImageBase64 = null

                        // Navigate to Home Screen
                        val intent = Intent(this@PostCaptionActivity, HomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@PostCaptionActivity, "Failed to upload post: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PostCaptionActivity", "Error getting user profile: ${e.message}", e)
                    Toast.makeText(this@PostCaptionActivity, "Error getting user info: ${e.message}", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                android.util.Log.e("PostCaptionActivity", "Error uploading post: ${e.message}", e)
                Toast.makeText(this@PostCaptionActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        captionEditText = null
        previewImageView = null
        postButton = null
        backButton = null
    }
}



