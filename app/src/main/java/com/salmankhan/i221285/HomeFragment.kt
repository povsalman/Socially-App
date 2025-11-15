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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.salmankhan.i221285.adapters.PostsAdapter
import com.salmankhan.i221285.models.Post
import com.salmankhan.i221285.AuthService
import com.salmankhan.i221285.services.PostService
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import de.hdodenhof.circleimageview.CircleImageView

class HomeFragment : Fragment() {
    
    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storiesAdapter: StoriesAdapter
    private val stories = mutableListOf<Story>()
    
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postsAdapter: PostsAdapter
    private val posts = mutableListOf<Post>()
    private var yourStoryCircle: CircleImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Find the dms ImageView and set click listener
        val dmsImageView = view.findViewById<ImageView>(R.id.dms)
        dmsImageView.setOnClickListener {
            val intent = Intent(activity, DmActivity::class.java)
            startActivity(intent)
        }

        // Find the camera ImageView and set click listener
        val cameraImageView = view.findViewById<ImageView>(R.id.camera)
        cameraImageView.setOnClickListener {
            val intent = Intent(activity, StoryTakeActivity::class.java)
            startActivity(intent)
        }

        // Configure "Your Story" quick upload button
        yourStoryCircle = view.findViewById(R.id.you)
        yourStoryCircle?.visibility = View.VISIBLE
        yourStoryCircle?.setOnClickListener {
            AuthService.currentUser()?.uid?.let { uid ->
                checkAndNavigateToStory(uid)
            } ?: run {
                Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            }
        }
        yourStoryCircle?.let { loadCurrentUserStoryButton(it) }
        updateYourStoryIndicator(hasActiveStory = false)

        // Setup stories RecyclerView
        setupStoriesRecyclerView(view)
        
        // Setup posts RecyclerView
        setupPostsRecyclerView(view)
        
        // Load stories and posts from Firebase
        loadStoriesFromFirebase()
        loadPostsFromFirebase()

        return view
    }
    
    private fun setupStoriesRecyclerView(view: View) {
        storiesRecyclerView = view.findViewById(R.id.stories_recycler_view)
        storiesRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        
        storiesAdapter = StoriesAdapter(
            context = requireContext(),
            stories = stories,
            onStoryClick = { story ->
                val intent = Intent(activity, StoryViewOwnActivity::class.java)
                intent.putExtra("story_id", story.id)
                intent.putExtra("user_id", story.userId)
                startActivity(intent)
            }
        )
        
        storiesRecyclerView.adapter = storiesAdapter
    }
    
    private fun setupPostsRecyclerView(view: View) {
        postsRecyclerView = view.findViewById(R.id.posts_recycler_view)
        postsRecyclerView.layoutManager = LinearLayoutManager(context)
        
        postsAdapter = PostsAdapter(
            context = requireContext(),
            posts = posts,
            onLikeClick = { post -> handleLike(post) },
            onCommentClick = { post -> handleComment(post) },
            onShareClick = { post -> handleShare(post) },
            onSaveClick = { post -> handleSave(post) }
        )
        
        postsRecyclerView.adapter = postsAdapter
    }
    
    private fun loadStoriesFromFirebase() {
        val currentUserId = AuthService.currentUser()?.uid
        if (currentUserId == null) {
            Log.e("HomeFragment", "Current user is null")
            return
        }
        
        Log.d("HomeFragment", "Loading stories for user: $currentUserId")
        
        StoryService.getStoriesForHomeFeed(currentUserId) { fetchedStories ->
            activity?.runOnUiThread {
                stories.clear()
                val otherStories = fetchedStories.filter { it.userId != currentUserId }
                stories.addAll(otherStories)
                storiesAdapter.notifyDataSetChanged()
                
                val hasOwnStory = fetchedStories.any { it.userId == currentUserId }
                updateYourStoryIndicator(hasOwnStory)
                yourStoryCircle?.let { loadCurrentUserStoryButton(it) }
                
                Log.d("HomeFragment", "Loaded ${stories.size} follower/following stories, hasOwnStory=$hasOwnStory")
                // Debug: Print each story's userId to verify
                fetchedStories.forEachIndexed { index, story ->
                    val label = if (index == 0 && story.userId == currentUserId) "YOUR STORY" else "FOLLOWED USER"
                    Log.d("HomeFragment", "[$label] Story from user: ${story.userId}, username: ${story.username}")
                }
            }
        }
    }
    
    private fun loadPostsFromFirebase() {
        lifecycleScope.launch {
            try {
                val currentUser = AuthService.currentUser()
                if (currentUser == null) {
                    Log.d("HomeFragment", "No logged in user")
                    return@launch
                }
                
                // Get list of users being followed
                val followingList = com.salmankhan.i221285.services.FollowService.getFollowingList(currentUser.uid)
                
                // Add current user to the list to show own posts
                val userIdsToShow = followingList.toMutableList()
                userIdsToShow.add(currentUser.uid)
                
                Log.d("HomeFragment", "Loading posts from ${userIdsToShow.size} users (following + self)")
                
                // Load posts from followed users + self
                if (userIdsToShow.isNotEmpty()) {
                    PostService.getPostsFromUsers(userIdsToShow) { fetchedPosts ->
                        activity?.runOnUiThread {
                            posts.clear()
                            posts.addAll(fetchedPosts)
                            postsAdapter.notifyDataSetChanged()
                            
                            Log.d("HomeFragment", "Loaded ${posts.size} posts from feed")
                        }
                    }
                } else {
                    // If not following anyone, just show own posts
                    PostService.getUserPosts(currentUser.uid) { fetchedPosts ->
                        activity?.runOnUiThread {
                            posts.clear()
                            posts.addAll(fetchedPosts)
                            postsAdapter.notifyDataSetChanged()
                            
                            Log.d("HomeFragment", "Loaded ${posts.size} own posts")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading posts: ${e.message}", e)
            }
        }
    }
    
    private fun handleLike(post: Post) {
        lifecycleScope.launch {
            try {
                val currentUser = AuthService.currentUser()
                if (currentUser == null) {
                    Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Check if already liked
                val hasLiked = PostService.hasUserLikedPost(post.id, currentUser.uid)
                
                if (hasLiked) {
                    // Unlike
                    val result = PostService.unlikePost(post.id, currentUser.uid)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Post unliked", Toast.LENGTH_SHORT).show()
                        loadPostsFromFirebase() // Refresh posts
                    }
                } else {
                    // Like
                    val result = PostService.likePost(post.id, currentUser.uid)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Post liked", Toast.LENGTH_SHORT).show()
                        loadPostsFromFirebase() // Refresh posts
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error handling like: ${e.message}", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleComment(post: Post) {
        // Navigate to CommentsActivity to view and add comments
        val intent = Intent(requireContext(), CommentsActivity::class.java)
        intent.putExtra("POST_ID", post.id)
        startActivity(intent)
    }
    
    private fun handleShare(post: Post) {
        Toast.makeText(context, "Share feature coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun handleSave(post: Post) {
        Toast.makeText(context, "Save feature coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun checkAndNavigateToStory(userId: String) {
        StoryService.getUserStories(userId) { userStories ->
            activity?.runOnUiThread {
                if (userStories.isNotEmpty()) {
                    // User has active stories, view them
                    val intent = Intent(activity, StoryViewOwnActivity::class.java)
                    intent.putExtra("user_id", userId)
                    startActivity(intent)
                } else {
                    // No active stories, create new one
                    val intent = Intent(activity, StoryTakeActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }
    
    private fun loadCurrentUserStoryButton(imageView: de.hdodenhof.circleimageview.CircleImageView) {
        val currentUserId = AuthService.currentUser()?.uid ?: return
        
        lifecycleScope.launch {
            try {
                // Load user's profile image
                val userRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUserId)
                
                val snapshot = userRef.get().await()
                val profileImageBase64 = snapshot.child("profileImage").getValue(String::class.java)
                
                activity?.runOnUiThread {
                    if (!profileImageBase64.isNullOrEmpty() && profileImageBase64 != "default") {
                        try {
                            val decodedBytes = android.util.Base64.decode(profileImageBase64, android.util.Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            imageView.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Error loading profile image", e)
                            imageView.setImageResource(R.drawable.person1)
                        }
                    } else {
                        imageView.setImageResource(R.drawable.person1)
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading user profile", e)
                activity?.runOnUiThread {
                    imageView.setImageResource(R.drawable.person1)
                }
            }
        }
    }
    
    private fun updateYourStoryIndicator(hasActiveStory: Boolean) {
        yourStoryCircle?.let { circle ->
            val borderColorRes = if (hasActiveStory) R.color.story_border_start else R.color.gray
            val borderWidth = if (hasActiveStory) 6 else 2
            
            val context = circle.context
            circle.borderColor = context.getColor(borderColorRes)
            circle.borderWidth = borderWidth
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh stories and posts when fragment becomes visible
        loadStoriesFromFirebase()
        loadPostsFromFirebase()
    }
}