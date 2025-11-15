package com.salmankhan.i221285.services

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.firebase.database.*
import com.salmankhan.i221285.models.Post
import com.salmankhan.i221285.models.Comment
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.*

/**
 * Service class for managing Posts, Likes, and Comments in Firebase Realtime Database
 */
object PostService {
    private const val TAG = "PostService"
    private val database = FirebaseDatabase.getInstance()
    private val postsRef = database.getReference("posts")
    private val likesRef = database.getReference("likes")
    private val commentsRef = database.getReference("comments")
    
    /**
     * Convert Bitmap to Base64 string
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    /**
     * Upload a new post to Firebase
     */
    suspend fun uploadPost(
        userId: String,
        username: String,
        userProfileImage: String,
        imageBase64: String,
        caption: String
    ): Result<String> {
        return try {
            val postId = UUID.randomUUID().toString()
            val post = Post(
                id = postId,
                userId = userId,
                username = username,
                userProfileImage = userProfileImage,
                imageBase64 = imageBase64,
                caption = caption,
                createdAt = System.currentTimeMillis()
            )
            
            postsRef.child(postId).setValue(post).await()
            
            // Update user's post count
            incrementUserPostCount(userId)
            
            Log.d(TAG, "Post uploaded successfully: $postId")
            Result.success(postId)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading post: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all posts (for explore/discovery)
     */
    fun getAllPosts(callback: (List<Post>) -> Unit) {
        postsRef.orderByChild("createdAt")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val posts = mutableListOf<Post>()
                    for (child in snapshot.children) {
                        val post = child.getValue(Post::class.java)
                        if (post != null) {
                            posts.add(post)
                        }
                    }
                    posts.sortByDescending { it.createdAt }
                    callback(posts)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error fetching posts: ${error.message}")
                    callback(emptyList())
                }
            })
    }
    
    /**
     * Get posts from specific users (for feed)
     */
    fun getPostsFromUsers(userIds: List<String>, callback: (List<Post>) -> Unit) {
        if (userIds.isEmpty()) {
            callback(emptyList())
            return
        }
        
        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = mutableListOf<Post>()
                for (child in snapshot.children) {
                    val post = child.getValue(Post::class.java)
                    if (post != null && post.userId in userIds) {
                        posts.add(post)
                    }
                }
                posts.sortByDescending { it.createdAt }
                callback(posts)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching user posts: ${error.message}")
                callback(emptyList())
            }
        })
    }
    
    /**
     * Get posts by a specific user
     */
    fun getUserPosts(userId: String, callback: (List<Post>) -> Unit) {
        postsRef.orderByChild("userId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val posts = mutableListOf<Post>()
                    for (child in snapshot.children) {
                        val post = child.getValue(Post::class.java)
                        if (post != null) {
                            posts.add(post)
                        }
                    }
                    posts.sortByDescending { it.createdAt }
                    callback(posts)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error fetching user posts: ${error.message}")
                    callback(emptyList())
                }
            })
    }
    
    /**
     * Like a post
     */
    suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            // Add like to likes/{postId}/{userId}
            likesRef.child(postId).child(userId).setValue(System.currentTimeMillis()).await()
            
            // Increment likes count on post
            postsRef.child(postId).child("likesCount").runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentValue = currentData.getValue(Int::class.java) ?: 0
                    currentData.value = currentValue + 1
                    return Transaction.success(currentData)
                }
                
                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        Log.e(TAG, "Error incrementing likes count: ${error.message}")
                    }
                }
            })
            
            Log.d(TAG, "Post liked successfully: $postId by $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error liking post: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Unlike a post
     */
    suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return try {
            // Remove like from likes/{postId}/{userId}
            likesRef.child(postId).child(userId).removeValue().await()
            
            // Decrement likes count on post
            postsRef.child(postId).child("likesCount").runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentValue = currentData.getValue(Int::class.java) ?: 0
                    currentData.value = maxOf(0, currentValue - 1)
                    return Transaction.success(currentData)
                }
                
                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        Log.e(TAG, "Error decrementing likes count: ${error.message}")
                    }
                }
            })
            
            Log.d(TAG, "Post unliked successfully: $postId by $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unliking post: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if user has liked a post
     */
    suspend fun hasUserLikedPost(postId: String, userId: String): Boolean {
        return try {
            val snapshot = likesRef.child(postId).child(userId).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if user liked post: ${e.message}", e)
            false
        }
    }
    
    /**
     * Add a comment to a post
     */
    suspend fun addComment(
        postId: String,
        userId: String,
        username: String,
        userProfileImage: String,
        commentText: String
    ): Result<String> {
        return try {
            val commentId = UUID.randomUUID().toString()
            val comment = Comment(
                id = commentId,
                postId = postId,
                userId = userId,
                username = username,
                userProfileImage = userProfileImage,
                text = commentText,
                createdAt = System.currentTimeMillis()
            )
            
            commentsRef.child(postId).child(commentId).setValue(comment).await()
            
            // Increment comments count on post
            postsRef.child(postId).child("commentsCount").runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentValue = currentData.getValue(Int::class.java) ?: 0
                    currentData.value = currentValue + 1
                    return Transaction.success(currentData)
                }
                
                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        Log.e(TAG, "Error incrementing comments count: ${error.message}")
                    }
                }
            })
            
            Log.d(TAG, "Comment added successfully: $commentId")
            Result.success(commentId)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding comment: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get comments for a post
     */
    fun getPostComments(postId: String, callback: (List<Comment>) -> Unit) {
        commentsRef.child(postId).orderByChild("createdAt")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val comments = mutableListOf<Comment>()
                    for (child in snapshot.children) {
                        val comment = child.getValue(Comment::class.java)
                        if (comment != null) {
                            comments.add(comment)
                        }
                    }
                    comments.sortBy { it.createdAt } // Oldest first
                    callback(comments)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error fetching comments: ${error.message}")
                    callback(emptyList())
                }
            })
    }
    
    /**
     * Delete a post
     */
    suspend fun deletePost(postId: String, userId: String): Result<Unit> {
        return try {
            // Verify post belongs to user
            val snapshot = postsRef.child(postId).get().await()
            val post = snapshot.getValue(Post::class.java)
            
            if (post != null && post.userId == userId) {
                // Delete post
                postsRef.child(postId).removeValue().await()
                
                // Delete all likes
                likesRef.child(postId).removeValue().await()
                
                // Delete all comments
                commentsRef.child(postId).removeValue().await()
                
                // Decrement user's post count
                decrementUserPostCount(userId)
                
                Log.d(TAG, "Post deleted successfully: $postId")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unauthorized or post not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting post: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Helper: Increment user's post count
     */
    private fun incrementUserPostCount(userId: String) {
        val userRef = database.getReference("users").child(userId).child("postsCount")
        userRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentValue = currentData.getValue(Int::class.java) ?: 0
                currentData.value = currentValue + 1
                return Transaction.success(currentData)
            }
            
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e(TAG, "Error incrementing user post count: ${error.message}")
                }
            }
        })
    }
    
    /**
     * Helper: Decrement user's post count
     */
    private fun decrementUserPostCount(userId: String) {
        val userRef = database.getReference("users").child(userId).child("postsCount")
        userRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentValue = currentData.getValue(Int::class.java) ?: 0
                currentData.value = maxOf(0, currentValue - 1)
                return Transaction.success(currentData)
            }
            
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e(TAG, "Error decrementing user post count: ${error.message}")
                }
            }
        })
    }
}




