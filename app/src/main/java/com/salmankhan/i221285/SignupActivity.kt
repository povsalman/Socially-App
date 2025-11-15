package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

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
        val emailEdit = findViewById<EditText>(R.id.emailEditText)
        val passwordEdit = findViewById<EditText>(R.id.passwordEditText)
        val usernameEdit = findViewById<EditText>(R.id.usernameEditText)
        val firstNameEdit = findViewById<EditText>(R.id.yourNameEditText)
        val lastNameEdit = findViewById<EditText>(R.id.yourLastNameEditText)
        createAccountBtn.setOnClickListener {
            val email = emailEdit.text?.toString()?.trim().orEmpty()
            val password = passwordEdit.text?.toString()?.trim().orEmpty()
            if (email.isEmpty() || password.isEmpty()) {
                android.widget.Toast.makeText(this, "Enter email and password", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    android.widget.Toast.makeText(this@SignupActivity, "Creating account...", android.widget.Toast.LENGTH_SHORT).show()
                    
                    // Try Firebase signup
                    try {
                        AuthService.signUpWithEmail(email, password)
                        android.widget.Toast.makeText(this@SignupActivity, "Account created successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        
                        val uid = AuthService.currentUser()?.uid
                        if (uid != null) {
                            android.widget.Toast.makeText(this@SignupActivity, "Setting up profile...", android.widget.Toast.LENGTH_SHORT).show()
                            AuthService.initializeUserProfileIfMissing(
                                uid,
                                usernameEdit.text?.toString(),
                                firstNameEdit.text?.toString(),
                                lastNameEdit.text?.toString()
                            )
                            android.widget.Toast.makeText(this@SignupActivity, "Profile setup complete!", android.widget.Toast.LENGTH_SHORT).show()
                            
                            // Navigate directly to HomeActivity
                            val intent = Intent(this@SignupActivity, HomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            android.widget.Toast.makeText(this@SignupActivity, "User creation failed - no UID", android.widget.Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(this@SignupActivity, "Firebase signup failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        android.util.Log.e("SignupActivity", "Firebase signup error: ${e.message}", e)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SignupActivity", "General signup error: ${e.message}", e)
                    android.widget.Toast.makeText(this@SignupActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
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
