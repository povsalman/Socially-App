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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.salmankhan.i221285.adapters.SearchUsersAdapter
import com.salmankhan.i221285.models.User
import com.salmankhan.i221285.network.ApiClient
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var clearButton: TextView
    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var filterAll: TextView
    private lateinit var filterFollowers: TextView
    private lateinit var filterFollowing: TextView

    private lateinit var searchAdapter: SearchUsersAdapter
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

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            searchResults.clear()
            searchAdapter.notifyDataSetChanged()
            showEmptyState(true)
            return
        }

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.searchUsers(query, "all")
                if (response.isSuccessful && response.body()?.success == true) {
                    var users = response.body()?.data?.users ?: emptyList()
                    val currentUserInt = currentUserId?.toIntOrNull()
                    if (currentUserInt != null) {
                        users = users.filter { it.userId != currentUserInt }
                    }

                    when (currentFilter) {
                        "Followers" -> {
                            val followerResponse = ApiClient.apiService.getFollowers(currentUserId ?: return@launch)
                            if (followerResponse.isSuccessful && followerResponse.body()?.success == true) {
                                val followerIds = followerResponse.body()?.data?.followers?.map { it.userId } ?: emptyList()
                                users = users.filter { followerIds.contains(it.userId) }
                            }
                        }
                        "Following" -> {
                            val followingResponse = ApiClient.apiService.getFollowing(currentUserId ?: return@launch)
                            if (followingResponse.isSuccessful && followingResponse.body()?.success == true) {
                                val followingIds = followingResponse.body()?.data?.following?.map { it.userId } ?: emptyList()
                                users = users.filter { followingIds.contains(it.userId) }
                            }
                        }
                    }

                    searchResults.clear()
                    searchResults.addAll(
                        users.map { apiUser ->
                            User(
                                uid = apiUser.userId.toString(),
                                username = apiUser.username,
                                firstName = apiUser.firstName ?: "",
                                lastName = apiUser.lastName ?: "",
                                profileImage = apiUser.profileImage ?: "",
                                postsCount = apiUser.postsCount,
                                followersCount = apiUser.followersCount,
                                followingCount = apiUser.followingCount
                            )
                        }
                    )
                    searchAdapter.notifyDataSetChanged()
                    showEmptyState(searchResults.isEmpty())

                    Log.d("SearchActivity", "Search results: ${searchResults.size} users for query: $query")
                } else {
                    Log.e("SearchActivity", "Error searching users: ${response.body()?.error}")
                    Toast.makeText(this@SearchActivity, "Error searching users", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SearchActivity", "Error searching users: ${e.message}", e)
                Toast.makeText(this@SearchActivity, "Error searching users", Toast.LENGTH_SHORT).show()
            }
        }
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