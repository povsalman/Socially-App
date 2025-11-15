package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_setup)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val user = AuthService.currentUser()
        if (user == null) {
            // No user logged in, go to signup
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
            return
        }

        val completeButton = findViewById<MaterialButton>(R.id.completeProfileButton)
        val bioEdit = findViewById<EditText>(R.id.bioEditText)
        val interestsEdit = findViewById<EditText>(R.id.interestsEditText)
        val backButton = findViewById<ImageButton>(R.id.backButton)

        completeButton.setOnClickListener {
            val bio = bioEdit.text?.toString()?.trim().orEmpty()
            val interests = interestsEdit.text?.toString()?.trim().orEmpty()

            lifecycleScope.launch {
                try {
                    // Update user profile with additional info
                    val uid = user.uid
                    val ref = Firebase.database.getReference("users").child(uid)
                    
                    val updates = mapOf(
                        "bio" to bio,
                        "interests" to interests,
                        "profileCompleted" to true,
                        "profileSetupCompletedAt" to System.currentTimeMillis()
                    )
                    
                    ref.updateChildren(updates).await()
                    
                    Toast.makeText(this@ProfileSetupActivity, "Profile completed!", Toast.LENGTH_SHORT).show()
                    
                    // Navigate to HomeActivity
                    val intent = Intent(this@ProfileSetupActivity, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                    
                } catch (e: Exception) {
                    Toast.makeText(this@ProfileSetupActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        backButton.setOnClickListener {
            // Sign out and go to signup
            AuthService.signOut()
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }
    }
}
