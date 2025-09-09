package com.salmankhan.i221285

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class OtherFollowingProfileActivity : AppCompatActivity() {
    private lateinit var followButton: MaterialButton
    private var isFollowing: Boolean = true // Track the current state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_other_following_profile)

        // Set window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize the follow button
        followButton = findViewById(R.id.followButton)

        // Set initial state
        updateFollowButton()

        // Set click listener to toggle state
        followButton.setOnClickListener {
            isFollowing = !isFollowing
            updateFollowButton()
        }
    }

    private fun updateFollowButton() {
        if (isFollowing) {
            followButton.text = getString(R.string.following_button)
            followButton.backgroundTintList = null                  // Use the default background from XML/style
            followButton.setTextColor(getColor(R.color.black)) // Match the default text color
        } else {
            followButton.text = getString(R.string.follow_button)
            followButton.backgroundTintList = getColorStateList(R.color.text_color) // Brown background
            followButton.setTextColor(getColor(R.color.white))                      // White text
        }
    }
}