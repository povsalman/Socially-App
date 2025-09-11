package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class StoryTakeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_story_take)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val takePhotoButton = findViewById<ImageView>(R.id.capture_button)
        takePhotoButton.setOnClickListener {
            val intent = Intent(this, StoryEditOwnActivity::class.java)
            startActivity(intent)
        }

        // Back arrow → Previous screen
        val backIcon = findViewById<ImageView>(R.id.close_icon)
        backIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


    }
}