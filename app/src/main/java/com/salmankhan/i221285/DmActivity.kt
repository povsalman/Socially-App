package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class DmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dm)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Bottom camera button click → StoryTakeActivity
        val bottomCameraButton = findViewById<MaterialButton>(R.id.bottom_camera)
        bottomCameraButton.setOnClickListener {
            val intent = Intent(this, StoryTakeActivity::class.java)
            startActivity(intent)
        }

        // First DM item click → PersonaldmActivity
        val firstDmItem = findViewById<LinearLayout>(R.id.first_dm_item)
        firstDmItem.setOnClickListener {
            val intent = Intent(this, PersonaldmActivity::class.java)
            startActivity(intent)
        }

        // Back arrow → Previous screen
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        backIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}
