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
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}