package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_search -> {
                    loadFragment(ExploreFragment())
                    true
                }
                R.id.nav_add -> {
                    loadFragment(PostImageFragment())
                    true
                }
                R.id.nav_likes -> {
                    loadFragment(NotificationYouFragment()) // Default to You tab
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(SelfProfileFragment())
                    true
                }
                else -> false
            }
        }

        // Handle intent extra to load OtherFollowingProfileFragment
        val fragmentToLoad = intent.getStringExtra("fragment_to_load")
        val forceFragmentLoad = intent.getBooleanExtra("force_fragment_load", false)
        if (forceFragmentLoad && fragmentToLoad == "OtherFollowingProfile") {
            bottomNavigationView.selectedItemId = R.id.nav_search // Set navigation first
            loadFragment(OtherFollowingProfileFragment())
        } else if (savedInstanceState == null) { // Only load default if not restoring state
            loadFragment(HomeFragment())
            bottomNavigationView.selectedItemId = R.id.nav_home
        }
    }

    private fun loadFragment(fragment: Fragment) {
        // Clear back stack when switching between bottom nav items
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // If there are fragments in back stack, pop them
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            // If we're at a root fragment, ask before exiting
            showExitConfirmation()
        }
    }

    private fun showExitConfirmation() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Exit App")
        builder.setMessage("Do you want to exit the app?")
        
        builder.setPositiveButton("Yes") { dialog, _ ->
            finish()
            dialog.dismiss()
        }
        
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
    }
    
    override fun onResume() {
        super.onResume()
        // Set user as online
        val userId = com.salmankhan.i221285.AuthService.currentUser()?.uid
        userId?.let { com.salmankhan.i221285.services.PresenceService.setUserOnline(it) }
    }
    
    override fun onPause() {
        super.onPause()
        // Set user as offline
        val userId = com.salmankhan.i221285.AuthService.currentUser()?.uid
        userId?.let { com.salmankhan.i221285.services.PresenceService.setUserOffline(it) }
    }
}