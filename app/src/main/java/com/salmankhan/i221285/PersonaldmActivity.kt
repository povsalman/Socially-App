package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class PersonaldmActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_personaldm)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        //video button click → VideocallActivity
        val VideoButton = findViewById<ImageView>(R.id.videocall_icon)
        VideoButton.setOnClickListener {
            val intent = Intent(this, VideocallActivity::class.java)
            startActivity(intent)
        }


        //camera button click → StoryTakeActivity
        val CameraButton = findViewById<ImageView>(R.id.camera_icon)
        CameraButton.setOnClickListener {
            val intent = Intent(this, StoryTakeActivity::class.java)
            startActivity(intent)
        }

        // Back arrow → Previous screen
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        backIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

    }
}