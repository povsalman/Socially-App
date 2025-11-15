package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.salmankhan.i221285.adapters.SearchUsersAdapter
import com.salmankhan.i221285.models.User
import com.salmankhan.i221285.services.FollowService
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FollowersListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var titleText: TextView
    private lateinit var adapter: SearchUsersAdapter
    private val followers = mutableListOf<User>()
    
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers_list)

        userId = intent.getStringExtra("user_id")
        val username = intent.getStringExtra("username") ?: "User"

        // Initialize views
        recyclerView = findViewById(R.id.followers_recycler_view)
        emptyState = findViewById(R.id.empty_state_text)
        titleText = findViewById(R.id.title_text)
        
        titleText.text = "$username's Followers"

        // Back button
        findViewById<ImageView>(R.id.back_button).setOnClickListener {
            finish()
        }

        // Setup RecyclerView
        setupRecyclerView()

        // Load followers
        loadFollowers()
    }

    private fun setupRecyclerView() {
        adapter = SearchUsersAdapter(followers) { user ->
            openUserProfile(user.uid)
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FollowersListActivity)
            adapter = this@FollowersListActivity.adapter
        }
    }

    private fun loadFollowers() {
        val uid = userId ?: return
        
        lifecycleScope.launch {
            try {
                val followerIds = FollowService.getFollowersList(uid)
                val users = mutableListOf<User>()
                
                for (followerId in followerIds) {
                    val userRef = FirebaseDatabase.getInstance().getReference("users").child(followerId)
                    val snapshot = userRef.get().await()
                    
                    if (snapshot.exists()) {
                        val user = User(
                            uid = followerId,
                            username = snapshot.child("username").getValue(String::class.java) ?: "",
                            firstName = snapshot.child("firstName").getValue(String::class.java) ?: "",
                            lastName = snapshot.child("lastName").getValue(String::class.java) ?: "",
                            profileImage = snapshot.child("profileImage").getValue(String::class.java) ?: ""
                        )
                        users.add(user)
                    }
                }
                
                runOnUiThread {
                    followers.clear()
                    followers.addAll(users)
                    adapter.notifyDataSetChanged()
                    
                    if (users.isEmpty()) {
                        recyclerView.visibility = View.GONE
                        emptyState.visibility = View.VISIBLE
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        emptyState.visibility = View.GONE
                    }
                    
                    Log.d("FollowersList", "Loaded ${users.size} followers")
                }
            } catch (e: Exception) {
                Log.e("FollowersList", "Error loading followers: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@FollowersListActivity, "Error loading followers", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openUserProfile(userId: String) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("fragment_to_load", "OtherFollowingProfile")
        intent.putExtra("other_user_id", userId)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("force_fragment_load", true)
        startActivity(intent)
    }
}

