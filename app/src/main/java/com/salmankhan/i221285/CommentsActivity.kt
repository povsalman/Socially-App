package com.salmankhan.i221285

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.salmankhan.i221285.adapters.CommentsAdapter
import com.salmankhan.i221285.models.Comment
import com.salmankhan.i221285.models.Post
import com.salmankhan.i221285.services.PostService
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CommentsActivity : AppCompatActivity() {

    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentsAdapter: CommentsAdapter
    private val comments = mutableListOf<Comment>()
    
    private var postId: String? = null
    private var post: Post? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        // Get post ID from intent
        postId = intent.getStringExtra("POST_ID")
        if (postId == null) {
            Toast.makeText(this, "Error: No post ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup UI
        setupViews()
        
        // Load comments
        loadComments()
    }

    private fun setupViews() {
        // Back button
        findViewById<ImageView>(R.id.back_button).setOnClickListener {
            finish()
        }

        // Comments RecyclerView
        commentsRecyclerView = findViewById(R.id.comments_recycler_view)
        commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        
        commentsAdapter = CommentsAdapter(this, comments)
        commentsRecyclerView.adapter = commentsAdapter

        // Post button
        val commentEditText = findViewById<EditText>(R.id.comment_input)
        val postButton = findViewById<TextView>(R.id.post_comment_button)
        
        postButton.setOnClickListener {
            val commentText = commentEditText.text.toString().trim()
            if (commentText.isNotEmpty()) {
                postComment(commentText)
                commentEditText.text.clear()
            } else {
                Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadComments() {
        postId?.let { id ->
            PostService.getPostComments(id) { fetchedComments ->
                runOnUiThread {
                    comments.clear()
                    comments.addAll(fetchedComments)
                    commentsAdapter.notifyDataSetChanged()
                    
                    // Update comments count
                    findViewById<TextView>(R.id.comments_count_text).text = 
                        "${comments.size} ${if (comments.size == 1) "comment" else "comments"}"
                    
                    Log.d("CommentsActivity", "Loaded ${comments.size} comments")
                }
            }
        }
    }

    private fun postComment(commentText: String) {
        lifecycleScope.launch {
            try {
                val currentUser = AuthService.currentUser()
                if (currentUser == null) {
                    Toast.makeText(this@CommentsActivity, "Please login first", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                postId?.let { id ->
                    // Get user info
                    val userRef = FirebaseDatabase.getInstance()
                        .getReference("users").child(currentUser.uid)
                    val snapshot = userRef.get().await()
                    val username = snapshot.child("username").getValue(String::class.java) ?: "User"
                    val userProfileImage = snapshot.child("profileImage").getValue(String::class.java) ?: ""

                    // Add comment
                    val result = PostService.addComment(
                        postId = id,
                        userId = currentUser.uid,
                        username = username,
                        userProfileImage = userProfileImage,
                        commentText = commentText
                    )

                    if (result.isSuccess) {
                        Toast.makeText(this@CommentsActivity, "Comment posted", Toast.LENGTH_SHORT).show()
                        loadComments() // Refresh comments
                    } else {
                        Toast.makeText(this@CommentsActivity, "Failed to post comment", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CommentsActivity", "Error posting comment: ${e.message}", e)
                Toast.makeText(this@CommentsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

