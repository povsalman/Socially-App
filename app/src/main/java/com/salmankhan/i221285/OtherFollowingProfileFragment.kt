package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import com.salmankhan.i221285.SociallyApplication
import com.salmankhan.i221285.services.FollowService
import com.salmankhan.i221285.services.PostService
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class OtherFollowingProfileFragment : Fragment() {

    private lateinit var followButton: MaterialButton
    private lateinit var profileImage: CircleImageView
    private lateinit var profileNameTop: TextView
    private lateinit var profileName: TextView
    private lateinit var profileBio: TextView
    private lateinit var profileWebsite: TextView
    private lateinit var postsCount: TextView
    private lateinit var followersCount: TextView
    private lateinit var followingCount: TextView
    private lateinit var postsRecyclerView: RecyclerView
    
    private lateinit var postsAdapter: ProfilePostsAdapter
    private val userPosts = mutableListOf<Post>()
    
    private var isFollowing: Boolean = false
    private var hasRequestPending: Boolean = false
    private var otherUserId: String? = null
    private var otherUsername: String? = null
    private var otherProfileImage: String? = null
    private val currentUserId: String? by lazy { AuthService.currentUser()?.uid }
    private val userRepository by lazy { UserRepository(SociallyApplication.getInstance()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_other_following_profile, container, false)

        // Get other user ID from activity intent
        otherUserId = activity?.intent?.getStringExtra("other_user_id")
        
        if (otherUserId == null) {
            Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return view
        }

        // Initialize views
        followButton = view.findViewById(R.id.followButton)
        profileImage = view.findViewById(R.id.other_profile_image)
        profileNameTop = view.findViewById(R.id.profile_name_top)
        profileName = view.findViewById(R.id.other_profile_name)
        profileBio = view.findViewById(R.id.other_profile_bio)
        profileWebsite = view.findViewById(R.id.other_profile_website)
        postsCount = view.findViewById(R.id.other_posts_count)
        followersCount = view.findViewById(R.id.other_followers_count)
        followingCount = view.findViewById(R.id.other_following_count)
        postsRecyclerView = view.findViewById(R.id.other_profile_posts_recycler_view)

        // Setup posts RecyclerView
        setupPostsRecyclerView()

        // Load user profile
        loadUserProfile()

        // Check follow status
        checkFollowStatus()
        
        // Send profile visit notification
        sendProfileVisitNotification()

        // Set click listener to toggle follow/unfollow
        followButton.setOnClickListener {
            toggleFollowStatus()
        }

        // Message button click → Open chat
        val messageButton = view.findViewById<MaterialButton>(R.id.message_button)
        messageButton.setOnClickListener {
            openChat()
        }

        // Back arrow → Previous screen
        val backIcon = view.findViewById<ImageView>(R.id.back_icon)
        backIcon.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Setup click listeners for followers/following
        postsCount.setOnClickListener {
            // Could open posts view
        }

        followersCount.setOnClickListener {
            val userId = otherUserId ?: return@setOnClickListener
            val username = profileNameTop.text.toString()
            openFollowersList(userId, username)
        }

        followingCount.setOnClickListener {
            val userId = otherUserId ?: return@setOnClickListener
            val username = profileNameTop.text.toString()
            openFollowingList(userId, username)
        }

        return view
    }

    private fun setupPostsRecyclerView() {
        postsAdapter = ProfilePostsAdapter(userPosts) { post ->
            Toast.makeText(context, "Post: ${post.caption}", Toast.LENGTH_SHORT).show()
        }
        postsRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = postsAdapter
        }
    }

    private fun loadUserProfile() {
        val userId = otherUserId ?: return
        
        lifecycleScope.launch {
            try {
                val result = userRepository.getUserProfile(userId.toIntOrNull())
                if (result.isSuccess) {
                    val profile = result.getOrNull()
                    if (profile != null) {
                        otherUsername = profile.username
                        otherProfileImage = profile.profileImage
                        
                        val followers = try {
                            FollowService.getFollowersCount(userId)
                        } catch (e: Exception) {
                            Log.e("OtherProfile", "Error getting followers count: ${e.message}", e)
                            0
                        }
                        
                        val following = try {
                            FollowService.getFollowingCount(userId)
                        } catch (e: Exception) {
                            Log.e("OtherProfile", "Error getting following count: ${e.message}", e)
                            0
                        }
                        
                        activity?.runOnUiThread {
                            profileNameTop.text = profile.username
                            
                            val fullName = "${profile.firstName.orEmpty()} ${profile.lastName.orEmpty()}".trim()
                            profileName.text = if (fullName.isNotEmpty()) fullName else profile.username
                            
                            val bio = profile.bio.orEmpty()
                            profileBio.text = if (bio.isNotEmpty()) bio else "No bio"
                            profileBio.visibility = if (bio.isNotEmpty()) View.VISIBLE else View.GONE
                            
                            val website = profile.website.orEmpty()
                            profileWebsite.text = website
                            profileWebsite.visibility = if (website.isNotEmpty()) View.VISIBLE else View.GONE
                            
                            postsCount.text = "${profile.postsCount}\nPosts"
                            followersCount.text = "$followers\nFollowers"
                            followingCount.text = "$following\nFollowing"
                            
                            if (!profile.profileImage.isNullOrEmpty()) {
                                val fixedUrl = ApiClient.fixImageUrl(profile.profileImage)
                                Picasso.get()
                                    .load(fixedUrl)
                                    .placeholder(R.drawable.person1)
                                    .error(R.drawable.person1)
                                    .into(profileImage)
                            } else {
                                profileImage.setImageResource(R.drawable.person1)
                            }
                            
                            Log.d("OtherProfile", "Loaded profile for: ${profile.username}")
                        }
                        
                        loadUserPosts(userId)
                    }
                } else {
                    Log.e("OtherProfile", "Error loading profile: ${result.exceptionOrNull()?.message}")
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Error loading profile", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("OtherProfile", "Error loading profile: ${e.message}", e)
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
                Log.d("OtherProfile", "Loaded ${posts.size} posts")
            }
        }
    }

    private fun checkFollowStatus() {
        val currentUid = currentUserId ?: return
        val otherUid = otherUserId ?: return

        lifecycleScope.launch {
            try {
                isFollowing = FollowService.isFollowing(currentUid, otherUid)
                hasRequestPending = FollowService.hasRequestPending(currentUid, otherUid)
                
                activity?.runOnUiThread {
                    updateFollowButton()
                }
            } catch (e: Exception) {
                Log.e("OtherProfile", "Error checking follow status: ${e.message}", e)
            }
        }
    }

    private fun toggleFollowStatus() {
        val currentUid = currentUserId ?: return
        val otherUid = otherUserId ?: return

        lifecycleScope.launch {
            try {
                followButton.isEnabled = false
                
                val result = when {
                    isFollowing -> {
                        // Unfollow
                        FollowService.unfollowUser(currentUid, otherUid)
                    }
                    else -> {
                        // Send follow request
                        val currentUserResult = userRepository.getUserProfile(currentUid.toIntOrNull())
                        val currentUser = currentUserResult.getOrNull()
                        val currentUsername = currentUser?.username ?: "User"
                        val currentProfileImage = currentUser?.profileImage ?: ""
                        
                        FollowService.sendFollowRequest(
                            fromUserId = currentUid,
                            toUserId = otherUid,
                            fromUsername = currentUsername,
                            fromUserProfileImage = currentProfileImage
                        )
                    }
                }

                if (result.isSuccess) {
                    // Update states
                    when {
                        isFollowing -> {
                            isFollowing = false
                            Toast.makeText(context, "Unfollowed", Toast.LENGTH_SHORT).show()
                        }
                        hasRequestPending -> {
                            Toast.makeText(context, "Follow request pending approval", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            hasRequestPending = true
                            Toast.makeText(context, "Follow request sent!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    activity?.runOnUiThread {
                        updateFollowButton()
                        loadUserProfile() // Refresh counts
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("OtherProfile", "Error toggling follow: ${e.message}", e)
                activity?.runOnUiThread {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                activity?.runOnUiThread {
                    followButton.isEnabled = true
                }
            }
        }
    }

    private fun updateFollowButton() {
        when {
            isFollowing -> {
                // Already following - show "Following" button
                followButton.text = "Following"
                followButton.backgroundTintList = null
                followButton.setTextColor(requireContext().getColor(R.color.black))
            }
            hasRequestPending -> {
                // Request pending - show "Requested" button
                followButton.text = "Requested"
                followButton.backgroundTintList = null
                followButton.setTextColor(requireContext().getColor(R.color.gray))
            }
            else -> {
                // Not following - show "Follow" button
                followButton.text = "Follow"
                followButton.backgroundTintList = requireContext().getColorStateList(R.color.text_color)
                followButton.setTextColor(requireContext().getColor(R.color.white))
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

    private fun openChat() {
        val currentUid = currentUserId ?: return
        val otherUid = otherUserId ?: return
        val otherName = profileNameTop.text.toString()

        lifecycleScope.launch {
            try {
                val result = com.salmankhan.i221285.services.MessageService.getOrCreateChat(currentUid, otherUid)
                if (result.isSuccess) {
                    val chatId = result.getOrNull()
                    activity?.runOnUiThread {
                        val intent = Intent(activity, PersonaldmActivity::class.java)
                        intent.putExtra("chat_id", chatId)
                        intent.putExtra("other_user_id", otherUid)
                        intent.putExtra("other_username", otherName)
                        startActivity(intent)
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Error creating chat", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("OtherProfile", "Error opening chat: ${e.message}", e)
                activity?.runOnUiThread {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun sendProfileVisitNotification() {
        val currentUid = currentUserId ?: return
        val otherUid = otherUserId ?: return
        
        // Don't send notification if visiting own profile
        if (currentUid == otherUid) {
            return
        }
        
        lifecycleScope.launch {
            try {
                val currentUserResult = userRepository.getUserProfile(currentUid.toIntOrNull())
                val currentUsername = currentUserResult.getOrNull()?.username ?: "Someone"
                
                com.salmankhan.i221285.services.NotificationHelper.sendProfileVisitNotification(
                    visitedUserId = otherUid,
                    visitorUsername = currentUsername
                )
            } catch (e: Exception) {
                Log.e("OtherProfile", "Error sending profile visit notification: ${e.message}", e)
            }
        }
    }
}
