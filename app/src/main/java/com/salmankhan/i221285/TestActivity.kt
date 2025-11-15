package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create a simple layout programmatically
        val textView = TextView(this)
        textView.text = "Test Activity - App is working!"
        textView.textSize = 20f
        setContentView(textView)
        
        // After 3 seconds, go to signup
        textView.postDelayed({
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}
