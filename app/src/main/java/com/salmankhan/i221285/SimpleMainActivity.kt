package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SimpleMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Simple splash screen - just wait 5 seconds and go to signup
        lifecycleScope.launch {
            delay(5000)
            val intent = Intent(this@SimpleMainActivity, SignupActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
