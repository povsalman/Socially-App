package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val goToProfile = findViewById<TextView>(R.id.go_to_profile)
        val circle1 = findViewById<TextView>(R.id.circle_1)

        val listener = View.OnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("fragment_to_load", "OtherFollowingProfile")
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // Bring existing instance to front
            intent.putExtra("force_fragment_load", true) // Add a flag to force fragment load
            startActivity(intent)
            finish() // Close SearchActivity to prevent back navigation to it
        }

        goToProfile?.setOnClickListener(listener)
        circle1?.setOnClickListener(listener)
    }
}