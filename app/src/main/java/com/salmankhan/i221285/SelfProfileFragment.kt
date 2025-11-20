package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.salmankhan.i221285.adapters.ProfilePostsAdapter
import com.salmankhan.i221285.models.Post
import com.salmankhan.i221285.network.ApiClient
import com.salmankhan.i221285.repository.UserRepository
import com.salmankhan.i221285.services.FollowService
import com.salmankhan.i221285.services.PostService
import com.salmankhan.i221285.SociallyApplication
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class SelfProfileFragment : Fragment() {

    private var currentUserId: String? = null
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postsAdapter: ProfilePostsAdapter
    private val userPosts = mutableListOf<Post>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_self_profile, container, false)

        val editProfileButton = view.findViewById<MaterialButton>(R.id.edit_profile_button)
        editProfileButton.setOnClickListener {
            // Handle edit profile button click
            val intent = Intent(activity, EditProfileActivity::class.java)
            startActivity(intent)
        }

        val highlightImage1 = view.findViewById<CircleImageView>(R.id.highlight_image_1)
        highlightImage1.setOnClickListener {
            // Handle highlight image click
            val intent = Intent(activity, ViewHighlightActivity::class.java)
            startActivity(intent)
        }

        // Setup posts RecyclerView
        setupPostsRecyclerView(view)

        // Load user profile data
        loadUserProfile(view)

        // Setup click listeners for followers/following
        view.findViewById<TextView>(R.id.followers_count_text)?.setOnClickListener {
            val userId = currentUserId ?: return@setOnClickListener
            val username = view.findViewById<TextView>(R.id.profile_username_text)?.text.toString()
            openFollowersList(userId, username)
        }

        view.findViewById<TextView>(R.id.following_count_text)?.setOnClickListener {
            val userId = currentUserId ?: return@setOnClickListener
            val username = view.findViewById<TextView>(R.id.profile_username_text)?.text.toString()
            openFollowingList(userId, username)
        }

        // Setup menu icon for logout
        view.findViewById<View>(R.id.menu_icon)?.setOnClickListener {
            showLogoutDialog()
        }

        return view
    }

    private fun setupPostsRecyclerView(view: View) {
        postsRecyclerView = view.findViewById(R.id.profile_posts_recycler_view)
        postsAdapter = ProfilePostsAdapter(userPosts) { post ->
            // Handle post click - could open post detail view
            Toast.makeText(context, "Post clicked: ${post.caption}", Toast.LENGTH_SHORT).show()
        }
        
        postsRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 3) // 3 columns grid
            adapter = postsAdapter
        }
    }

    private fun loadUserProfile(view: View) {
        lifecycleScope.launch {
            try {
                val currentUser = AuthService.currentUser()
                if (currentUser == null) {
                    Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val userId = AuthService.getCurrentUserId() ?: return@launch
                currentUserId = userId

                // Get user data from repository
                val userRepository = UserRepository(SociallyApplication.getInstance())
                val userResult = userRepository.getUserProfile(userId.toIntOrNull())

                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()
                    if (user != null) {
                        // Get counts - wrapped in try-catch to prevent crashes
                        val followersCount = try {
                            FollowService.getFollowersCount(userId)
                        } catch (e: Exception) {
                            Log.e("SelfProfileFragment", "Error getting followers count: ${e.message}", e)
                            0
                        }
                        
                        val followingCount = try {
                            FollowService.getFollowingCount(userId)
                        } catch (e: Exception) {
                            Log.e("SelfProfileFragment", "Error getting following count: ${e.message}", e)
                            0
                        }

                        // Update UI
                        activity?.runOnUiThread {
                            // Set username
                            view.findViewById<TextView>(R.id.profile_username_text)?.text = user.username

                            // Set profile picture
                            val profileImageView = view.findViewById<CircleImageView>(R.id.profile_image)
                            if (!user.profileImage.isNullOrEmpty()) {
                                val fixedUrl = ApiClient.fixImageUrl(user.profileImage)
                                Picasso.get()
                                    .load(fixedUrl)
                                    .placeholder(R.drawable.person1)
                                    .error(R.drawable.person1)
                                    .into(profileImageView)
                            } else {
                                profileImageView?.setImageResource(R.drawable.person1)
                            }

                            // Set stats (posts count will be updated after loading posts)
                            view.findViewById<TextView>(R.id.posts_count_text)?.text = "0\nPosts"
                            view.findViewById<TextView>(R.id.followers_count_text)?.text = "$followersCount\nFollowers"
                            view.findViewById<TextView>(R.id.following_count_text)?.text = "$followingCount\nFollowing"

                            // Set name
                            val fullName = "${user.firstName.orEmpty()} ${user.lastName.orEmpty()}".trim()
                            if (fullName.isNotEmpty()) {
                                view.findViewById<TextView>(R.id.profile_name_text)?.text = fullName
                            }

                            // Set bio
                            view.findViewById<TextView>(R.id.profile_bio_text)?.text = user.bio ?: "No bio yet"

                            Log.d("SelfProfileFragment", "Loaded profile for: ${user.username}")
                        }

                        // Load user's posts
                        loadUserPosts(userId)
                    }
                } else {
                    Log.e("SelfProfileFragment", "User profile not found in database")
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Error loading profile", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("SelfProfileFragment", "Error loading profile: ${e.message}", e)
                activity?.runOnUiThread {
                    Toast.makeText(context, "Error loading profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadUserPosts(userId: String) {
        PostService.getUserPosts(userId) { posts ->
            activity?.runOnUiThread {
                userPosts.clear()
                userPosts.addAll(posts)
                postsAdapter.notifyDataSetChanged()
                
                Log.d("SelfProfileFragment", "Loaded ${posts.size} posts for user: $userId")
            }
        }
    }

    private fun openFollowersList(userId: String, username: String) {
        val intent = Intent(activity, FollowersListActivity::class.java)
        intent.putExtra("user_id", userId)
        intent.putExtra("username", username)
        startActivity(intent)
    }

    private fun openFollowingList(userId: String, username: String) {
        val intent = Intent(activity, FollowingListActivity::class.java)
        intent.putExtra("user_id", userId)
        intent.putExtra("username", username)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile when coming back to this fragment
        view?.let { loadUserProfile(it) }
    }

    private fun showLogoutDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")
        
        builder.setPositiveButton("Logout") { dialog, _ ->
            // Sign out from Firebase
            AuthService.signOut()
            
            // Navigate to login page
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
            
            dialog.dismiss()
        }
        
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
    }
}