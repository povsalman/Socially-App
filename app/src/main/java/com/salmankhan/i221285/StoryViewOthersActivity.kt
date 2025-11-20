package com.salmankhan.i221285

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.salmankhan.i221285.network.ApiClient
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class StoryViewOthersActivity : AppCompatActivity() {
    
    private var currentStory: Story? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_story_view_others)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get story ID from intent
        val storyId = intent.getStringExtra("story_id")
        val userId = intent.getStringExtra("user_id")
        
        if (storyId != null && userId != null) {
            loadStoryById(storyId, userId)
        } else if (userId != null) {
            loadUserLatestStory(userId)
        } else {
            Toast.makeText(this, "Error loading story", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Back arrow → Previous screen
        val backIcon = findViewById<ImageView>(R.id.close_icon)
        backIcon.setOnClickListener {
            finish()
        }
    }
    
    private fun loadStoryById(storyId: String, userId: String) {
        StoryService.getUserStories(userId) { stories ->
            runOnUiThread {
                val story = stories.find { it.id == storyId }
                if (story != null && !story.isExpired()) {
                    displayStory(story)
                } else {
                    Toast.makeText(this, "Story expired or not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
    
    private fun loadUserLatestStory(userId: String) {
        StoryService.getUserStories(userId) { stories ->
            runOnUiThread {
                if (stories.isNotEmpty()) {
                    displayStory(stories.first())
                } else {
                    Toast.makeText(this, "No active stories", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
    
    private fun displayStory(story: Story) {
        runOnUiThread {
            currentStory = story
            val storyImage = findViewById<ImageView>(R.id.story_image)
            val profileImage = findViewById<CircleImageView>(R.id.story_profile_image)
            val usernameText = findViewById<TextView>(R.id.story_username)
            val timeText = findViewById<TextView>(R.id.story_time)
            
            // Check if views are null
            if (storyImage == null || profileImage == null || usernameText == null || timeText == null) {
                Log.e("StoryViewOthersActivity", "Views are null - cannot display story")
                Toast.makeText(this, "Error loading story view", Toast.LENGTH_SHORT).show()
                finish()
                return@runOnUiThread
            }
            
            Log.d("StoryViewOthersActivity", "Displaying story - imageBase64: ${story.imageBase64.take(50)}...")
            
            // Display story image
            if (!story.imageBase64.isNullOrEmpty()) {
                if (story.imageBase64.startsWith("http://") || story.imageBase64.startsWith("https://")) {
                    val fixedStoryUrl = ApiClient.fixImageUrl(story.imageBase64)
                    Log.d("StoryViewOthersActivity", "Loading story image from URL: $fixedStoryUrl")
                    Picasso.get()
                        .load(fixedStoryUrl)
                        .fit()
                        .centerCrop()
                        .placeholder(R.drawable.person1)
                        .error(R.drawable.person1)
                        .into(storyImage, object : com.squareup.picasso.Callback {
                            override fun onSuccess() {
                                Log.d("StoryViewOthersActivity", "Story image loaded successfully")
                            }
                            override fun onError(e: Exception?) {
                                Log.e("StoryViewOthersActivity", "Error loading story image from URL", e)
                            }
                        })
                } else {
                    // Try to decode as base64
                    try {
                        val decodedBytes = Base64.decode(story.imageBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        if (bitmap != null) {
                            storyImage.setImageBitmap(bitmap)
                            Log.d("StoryViewOthersActivity", "Story image decoded from base64")
                        } else {
                            Log.e("StoryViewOthersActivity", "Failed to decode bitmap from base64")
                            storyImage.setImageResource(R.drawable.person1)
                        }
                    } catch (e: Exception) {
                        Log.e("StoryViewOthersActivity", "Error decoding story image from base64", e)
                        storyImage.setImageResource(R.drawable.person1)
                    }
                }
            } else {
                Log.w("StoryViewOthersActivity", "Story image is null or empty")
                storyImage.setImageResource(R.drawable.person1)
            }
            
            // Display profile image
            if (!story.userProfileImage.isNullOrEmpty() && story.userProfileImage != "default" && profileImage != null) {
                if (story.userProfileImage.startsWith("http://") || story.userProfileImage.startsWith("https://")) {
                    val fixedProfileUrl = ApiClient.fixImageUrl(story.userProfileImage)
                    Picasso.get()
                        .load(fixedProfileUrl)
                        .placeholder(R.drawable.person1)
                        .error(R.drawable.person1)
                        .into(profileImage)
                } else {
                    try {
                        val decodedBytes = Base64.decode(story.userProfileImage, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        if (bitmap != null) {
                            profileImage.setImageBitmap(bitmap)
                        }
                    } catch (e: Exception) {
                        Log.e("StoryViewOthersActivity", "Error decoding profile image", e)
                    }
                }
            } else if (profileImage != null) {
                profileImage.setImageResource(R.drawable.person1)
            }
            
            // Display username
            usernameText.text = story.username
            
            // Display time ago
            val hoursAgo = (System.currentTimeMillis() - story.createdAt) / (1000 * 60 * 60)
            timeText.text = if (hoursAgo < 1) "Just now" else "${hoursAgo}h ago"
        }
    }
}