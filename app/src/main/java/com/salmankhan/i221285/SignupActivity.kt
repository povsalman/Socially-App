package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Create Account → SwitchAccountsActivity
        val createAccountBtn = findViewById<MaterialButton>(R.id.createAccountButton)
        createAccountBtn.setOnClickListener {
            goToSwitchAccounts()
        }

        // Back arrow → SwitchAccountsActivity
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            goToSwitchAccounts()
        }

        // Handle device/system back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goToSwitchAccounts()
            }
        })
    }

    private fun goToSwitchAccounts() {
        val intent = Intent(this, SwitchAccountsActivity::class.java)
        // Clear SignupActivity from stack
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
