package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Login with FirebaseAuth
        val loginButton = findViewById<Button>(R.id.loginButton)
        val emailEdit = findViewById<android.widget.EditText>(R.id.loginEmailEditText)
        val passwordEdit = findViewById<android.widget.EditText>(R.id.loginPasswordEditText)
        loginButton.setOnClickListener {
            val email = emailEdit.text?.toString()?.trim().orEmpty()
            val password = passwordEdit.text?.toString()?.trim().orEmpty()
            if (email.isEmpty() || password.isEmpty()) {
                android.widget.Toast.makeText(this, "Enter email and password", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Disable button immediately to prevent multiple clicks
            loginButton.isEnabled = false
            
            android.widget.Toast.makeText(this@LoginActivity, "Logging in...", android.widget.Toast.LENGTH_SHORT).show()
            android.util.Log.d("LoginActivity", "=== LOGIN ATTEMPT START ===")
            android.util.Log.d("LoginActivity", "Email: $email")
            
            lifecycleScope.launch {
                try {
                    android.util.Log.d("LoginActivity", "Inside coroutine, calling AuthService.signInWithEmail")
                    
                    // Try Firebase authentication
                    AuthService.signInWithEmail(email, password)
                    
                    android.util.Log.d("LoginActivity", "AuthService.signInWithEmail returned (no exception)")
                    
                    // Check if user is actually signed in
                    val currentUser = AuthService.currentUser()
                    android.util.Log.d("LoginActivity", "Current user check: ${if (currentUser != null) "USER FOUND (${currentUser.uid})" else "USER IS NULL"}")
                    
                    if (currentUser != null) {
                        runOnUiThread {
                            android.util.Log.d("LoginActivity", "Running on UI thread - showing toast and navigating")
                            android.widget.Toast.makeText(this@LoginActivity, "Welcome back!", android.widget.Toast.LENGTH_SHORT).show()
                            
                            android.util.Log.d("LoginActivity", "Creating intent for HomeActivity")
                            val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            
                            android.util.Log.d("LoginActivity", "Starting HomeActivity")
                            startActivity(intent)
                            
                            android.util.Log.d("LoginActivity", "Finishing LoginActivity")
                            finish()
                            
                            android.util.Log.d("LoginActivity", "=== LOGIN SUCCESS ===")
                        }
                    } else {
                        runOnUiThread {
                            android.util.Log.e("LoginActivity", "User is null after sign-in - RE-ENABLING BUTTON")
                            android.widget.Toast.makeText(this@LoginActivity, "Login failed: User not found", android.widget.Toast.LENGTH_LONG).show()
                            loginButton.isEnabled = true
                        }
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.e("LoginActivity", "=== EXCEPTION CAUGHT ===")
                    android.util.Log.e("LoginActivity", "Exception type: ${e.javaClass.simpleName}")
                    android.util.Log.e("LoginActivity", "Exception message: ${e.message}")
                    android.util.Log.e("LoginActivity", "Stack trace:", e)
                    
                    runOnUiThread {
                        android.widget.Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        loginButton.isEnabled = true
                    }
                }
            }
        }

        // Navigate to SignupActivity when "Sign up" is clicked
        val signUpText = findViewById<TextView>(R.id.sign_up_text)
        signUpText.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        // Back arrow → SwitchAccountsActivity
        val backButton = findViewById<ImageView>(R.id.backButton)
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