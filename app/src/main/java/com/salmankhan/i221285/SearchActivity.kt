package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.salmankhan.i221285.adapters.SearchUsersAdapter
import com.salmankhan.i221285.models.User

class SearchActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var clearButton: TextView
    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var filterAll: TextView
    private lateinit var filterFollowers: TextView
    private lateinit var filterFollowing: TextView

    private lateinit var searchAdapter: SearchUsersAdapter
    private val allUsers = mutableListOf<User>()
    private val searchResults = mutableListOf<User>()
    private var currentFilter = "All" // All, Followers, Following

    private val currentUserId: String? by lazy { AuthService.currentUser()?.uid }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        searchInput = findViewById(R.id.search_input)
        clearButton = findViewById(R.id.clear_search_button)
        searchRecyclerView = findViewById(R.id.search_results_recycler_view)
        emptyStateText = findViewById(R.id.empty_state_text)
        filterAll = findViewById(R.id.filter_all)
        filterFollowers = findViewById(R.id.filter_followers)
        filterFollowing = findViewById(R.id.filter_following)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup search input
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Clear button
        clearButton.setOnClickListener {
            searchInput.text.clear()
            searchResults.clear()
            searchAdapter.notifyDataSetChanged()
            showEmptyState(true)
        }

        // Filter buttons
        filterAll.setOnClickListener {
            setActiveFilter("All")
            performSearch(searchInput.text.toString())
        }

        filterFollowers.setOnClickListener {
            setActiveFilter("Followers")
            performSearch(searchInput.text.toString())
        }

        filterFollowing.setOnClickListener {
            setActiveFilter("Following")
            performSearch(searchInput.text.toString())
        }

        // Load all users
        loadAllUsers()

        showEmptyState(true)
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchUsersAdapter(searchResults) { user ->
            // Navigate to user profile
            openUserProfile(user)
        }
        searchRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = searchAdapter
        }
    }

    private fun setActiveFilter(filter: String) {
        currentFilter = filter

        // Reset all filters
        filterAll.setTextColor(getColor(R.color.black))
        filterAll.setBackgroundResource(R.drawable.edittext_border)
        filterFollowers.setTextColor(getColor(R.color.black))
        filterFollowers.setBackgroundResource(R.drawable.edittext_border)
        filterFollowing.setTextColor(getColor(R.color.black))
        filterFollowing.setBackgroundResource(R.drawable.edittext_border)

        // Set active filter
        when (filter) {
            "All" -> {
                filterAll.setTextColor(getColor(R.color.white))
                filterAll.setBackgroundColor(getColor(R.color.text_color))
            }
            "Followers" -> {
                filterFollowers.setTextColor(getColor(R.color.white))
                filterFollowers.setBackgroundColor(getColor(R.color.text_color))
            }
            "Following" -> {
                filterFollowing.setTextColor(getColor(R.color.white))
                filterFollowing.setBackgroundColor(getColor(R.color.text_color))
            }
        }
    }

    private fun loadAllUsers() {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsers.clear()
                for (child in snapshot.children) {
                    try {
                        val user = User(
                            uid = child.key ?: "",
                            username = child.child("username").getValue(String::class.java) ?: "",
                            firstName = child.child("firstName").getValue(String::class.java) ?: "",
                            lastName = child.child("lastName").getValue(String::class.java) ?: "",
                            bio = child.child("bio").getValue(String::class.java) ?: "",
                            website = child.child("website").getValue(String::class.java) ?: "",
                            profileImage = child.child("profileImage").getValue(String::class.java) ?: "",
                            postsCount = child.child("postsCount").getValue(Int::class.java) ?: 0,
                            followersCount = child.child("followersCount").getValue(Int::class.java) ?: 0,
                            followingCount = child.child("followingCount").getValue(Int::class.java) ?: 0
                        )
                        
                        // Don't add current user to search results
                        if (user.uid != currentUserId) {
                            allUsers.add(user)
                        }
                    } catch (e: Exception) {
                        Log.e("SearchActivity", "Error parsing user: ${e.message}", e)
                    }
                }
                Log.d("SearchActivity", "Loaded ${allUsers.size} users")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SearchActivity", "Error loading users: ${error.message}")
                Toast.makeText(this@SearchActivity, "Error loading users", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            searchResults.clear()
            searchAdapter.notifyDataSetChanged()
            showEmptyState(true)
            return
        }

        val queryLower = query.lowercase()
        
        when (currentFilter) {
            "All" -> {
                // Search all users
                searchResults.clear()
                searchResults.addAll(
                    allUsers.filter { it.username.lowercase().contains(queryLower) }
                )
            }
            "Followers" -> {
                // Search only in followers
                filterByFollowersAndSearch(queryLower)
            }
            "Following" -> {
                // Search only in following
                filterByFollowingAndSearch(queryLower)
            }
        }

        searchAdapter.notifyDataSetChanged()
        showEmptyState(searchResults.isEmpty() && query.isNotEmpty())
        
        Log.d("SearchActivity", "Search results: ${searchResults.size} users for query: $query")
    }

    private fun filterByFollowersAndSearch(query: String) {
        val userId = currentUserId ?: return
        val followersRef = FirebaseDatabase.getInstance().getReference("followers").child(userId)
        
        followersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followerIds = snapshot.children.mapNotNull { it.key }
                searchResults.clear()
                searchResults.addAll(
                    allUsers.filter { user ->
                        followerIds.contains(user.uid) && user.username.lowercase().contains(query)
                    }
                )
                searchAdapter.notifyDataSetChanged()
                showEmptyState(searchResults.isEmpty())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SearchActivity", "Error filtering followers: ${error.message}")
            }
        })
    }

    private fun filterByFollowingAndSearch(query: String) {
        val userId = currentUserId ?: return
        val followingRef = FirebaseDatabase.getInstance().getReference("following").child(userId)
        
        followingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followingIds = snapshot.children.mapNotNull { it.key }
                searchResults.clear()
                searchResults.addAll(
                    allUsers.filter { user ->
                        followingIds.contains(user.uid) && user.username.lowercase().contains(query)
                    }
                )
                searchAdapter.notifyDataSetChanged()
                showEmptyState(searchResults.isEmpty())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SearchActivity", "Error filtering following: ${error.message}")
            }
        })
    }

    private fun showEmptyState(show: Boolean) {
        if (show) {
            emptyStateText.visibility = View.VISIBLE
            searchRecyclerView.visibility = View.GONE
        } else {
            emptyStateText.visibility = View.GONE
            searchRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun openUserProfile(user: User) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("fragment_to_load", "OtherFollowingProfile")
        intent.putExtra("other_user_id", user.uid)
        intent.putExtra("force_fragment_load", true)
        startActivity(intent)
        // Don't call finish() - allow back navigation
    }
}