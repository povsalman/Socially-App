package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SimpleLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailEdit = findViewById<TextInputEditText>(R.id.loginEmailEditText)
        val passwordEdit = findViewById<TextInputEditText>(R.id.loginPasswordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val email = emailEdit.text?.toString()?.trim().orEmpty()
            val password = passwordEdit.text?.toString()?.trim().orEmpty()
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    Toast.makeText(this@SimpleLoginActivity, "Logging in...", Toast.LENGTH_SHORT).show()
                    AuthService.signInWithEmail(email, password)
                    Toast.makeText(this@SimpleLoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                    
                    // Go directly to ProfileSetupActivity
                    val intent = Intent(this@SimpleLoginActivity, ProfileSetupActivity::class.java)
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@SimpleLoginActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Navigate to SignupActivity when "Sign up" is clicked
        val signUpText = findViewById<TextView>(R.id.sign_up_text)
        signUpText.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }
}
