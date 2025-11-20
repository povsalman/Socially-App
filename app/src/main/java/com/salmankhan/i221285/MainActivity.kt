//package com.salmankhan.i221285
//
//import android.os.Bundle
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//
//class MainActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//    }
//}


package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Splash screen: Show for 5 seconds, then navigate based on login state
        lifecycleScope.launch {
            delay(5000) // Show splash for 5 seconds as required
            
            val currentUser = AuthService.currentUser()
            val userId = AuthService.getCurrentUserId()
            
            if (currentUser != null && userId != null) {
                // User is logged in - check if profile is completed
                try {
                    val profileCompleted = AuthService.isProfileCompleted(userId)
                    
                    Log.d("MainActivity", "User logged in. Profile completed: $profileCompleted")
                    
                    if (profileCompleted) {
                        // Navigate to HomeActivity
                        val intent = Intent(this@MainActivity, HomeActivity::class.java)
                        startActivity(intent)
                    } else {
                        // Profile not completed - go to ProfileSetupActivity
                        val intent = Intent(this@MainActivity, ProfileSetupActivity::class.java)
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error checking profile: ${e.message}", e)
                    // On error, logout and go to login page
                    AuthService.signOut()
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    startActivity(intent)
                }
            } else {
                // User not logged in - navigate to LoginActivity
                Log.d("MainActivity", "User not logged in. Redirecting to Login page")
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
            }
            
            finish()
        }
    }
}


