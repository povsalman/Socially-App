package com.salmankhan.i221285

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import android.util.Log
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

/**
 * Service class for managing Stories in Firebase Realtime Database
 * Handles upload, fetch, and auto-deletion of stories
 */
object StoryService {
    private const val TAG = "StoryService"
    private val database = FirebaseDatabase.getInstance()
    private val storiesRef = database.getReference("stories")
    
    /**
     * Fix image orientation based on EXIF data
     */
    fun fixImageOrientation(bitmap: Bitmap, imageUri: android.net.Uri?, contentResolver: android.content.ContentResolver?): Bitmap {
        if (imageUri == null || contentResolver == null) return bitmap
        
        return try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val exif = ExifInterface(inputStream!!)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
            }
            
            if (matrix.isIdentity) {
                bitmap
            } else {
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fixing image orientation: ${e.message}")
            bitmap
        }
    }
    
    /**
     * Convert Bitmap to Base64 string for Firebase storage
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream) // 80% quality to reduce size
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    /**
     * Convert Base64 string back to Bitmap
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting base64 to bitmap: ${e.message}")
            null
        }
    }
    
    /**
     * Upload a new story to Firebase
     */
    suspend fun uploadStory(
        userId: String,
        username: String,
        userProfileImage: String,
        imageBase64: String
    ): Result<String> {
        return try {
            val storyId = UUID.randomUUID().toString()
            val story = Story(
                id = storyId,
                userId = userId,
                username = username,
                userProfileImage = userProfileImage,
                imageBase64 = imageBase64
            )
            
            storiesRef.child(storyId).setValue(story).await()
            Log.d(TAG, "Story uploaded successfully: $storyId")
            Result.success(storyId)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading story: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all active stories (not expired) for the home feed
     */
    fun getAllActiveStories(callback: (List<Story>) -> Unit) {
        storiesRef.orderByChild("isActive").equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val stories = mutableListOf<Story>()
                    for (child in snapshot.children) {
                        val story = child.getValue(Story::class.java)
                        if (story != null && !story.isExpired()) {
                            stories.add(story)
                        } else if (story != null && story.isExpired()) {
                            // Auto-delete expired stories
                            deleteStory(story.id)
                        }
                    }
                    // Sort by creation time (newest first)
                    stories.sortByDescending { it.createdAt }
                    callback(stories)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error fetching stories: ${error.message}")
                    callback(emptyList())
                }
            })
    }
    
    /**
     * Get stories for home feed:
     * - Current user's story (if exists) is first
     * - Stories from follower & following accounts next (newest first, one per user)
     */
    fun getStoriesForHomeFeed(currentUserId: String, callback: (List<Story>) -> Unit) {
        getRelatedUserIds(currentUserId) { relatedUserIds ->
            val allowedUserIds = relatedUserIds.toMutableSet()
            allowedUserIds.add(currentUserId)
            
            storiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentUserStories = mutableListOf<Story>()
                    val otherUserStories = mutableMapOf<String, Story>()
                    
                    for (child in snapshot.children) {
                        val story = child.getValue(Story::class.java)
                        
                        if (story != null) {
                            val isAllowedUser = allowedUserIds.contains(story.userId)
                            if (!isAllowedUser) {
                                continue
                            }
                            
                            if (story.isExpired()) {
                                deleteStory(story.id)
                                continue
                            }
                            
                            if (!story.isActive) {
                                continue
                            }
                            
                            if (story.userId == currentUserId) {
                                currentUserStories.add(story)
                            } else {
                                val existingStory = otherUserStories[story.userId]
                                if (existingStory == null || story.createdAt > existingStory.createdAt) {
                                    otherUserStories[story.userId] = story
                                }
                            }
                        }
                    }
                    
                    currentUserStories.sortByDescending { it.createdAt }
                    
                    val resultStories = mutableListOf<Story>()
                    currentUserStories.firstOrNull()?.let { resultStories.add(it) }
                    resultStories.addAll(otherUserStories.values.sortedByDescending { it.createdAt })
                    
                    Log.d(TAG, "Stories for feed: current=${currentUserStories.size}, others=${otherUserStories.size}")
                    callback(resultStories)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading stories: ${error.message}")
                    callback(emptyList())
                }
            })
        }
    }

    /**
     * Helper to merge follower + following user IDs for story visibility
     */
    private fun getRelatedUserIds(currentUserId: String, callback: (Set<String>) -> Unit) {
        val relatedIds = mutableSetOf<String>()
        val followingRef = database.getReference("following").child(currentUserId)
        val followersRef = database.getReference("followers").child(currentUserId)
        
        var pendingCalls = 2
        
        fun checkCompletion() {
            pendingCalls--
            if (pendingCalls <= 0) {
                callback(relatedIds)
            }
        }
        
        val followingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    child.key?.let { relatedIds.add(it) }
                }
                Log.d(TAG, "Fetched ${snapshot.childrenCount} following users")
                checkCompletion()
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching following list: ${error.message}")
                checkCompletion()
            }
        }
        
        val followersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    child.key?.let { relatedIds.add(it) }
                }
                Log.d(TAG, "Fetched ${snapshot.childrenCount} follower users")
                checkCompletion()
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching followers list: ${error.message}")
                checkCompletion()
            }
        }
        
        followingRef.addListenerForSingleValueEvent(followingListener)
        followersRef.addListenerForSingleValueEvent(followersListener)
    }
    
    /**
     * Get stories for a specific user
     */
    fun getUserStories(userId: String, callback: (List<Story>) -> Unit) {
        storiesRef.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val stories = mutableListOf<Story>()
                    for (child in snapshot.children) {
                        val story = child.getValue(Story::class.java)
                        if (story != null && !story.isExpired()) {
                            stories.add(story)
                        } else if (story != null && story.isExpired()) {
                            // Auto-delete expired stories
                            deleteStory(story.id)
                        }
                    }
                    stories.sortByDescending { it.createdAt }
                    callback(stories)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error fetching user stories: ${error.message}")
                    callback(emptyList())
                }
            })
    }
    
    /**
     * Delete a story by ID
     */
    fun deleteStory(storyId: String) {
        storiesRef.child(storyId).removeValue()
            .addOnSuccessListener {
                Log.d(TAG, "Story deleted successfully: $storyId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting story: ${e.message}")
            }
    }
    
    /**
     * Clean up expired stories (call this periodically)
     */
    fun cleanupExpiredStories() {
        storiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val story = child.getValue(Story::class.java)
                    if (story != null && story.isExpired()) {
                        deleteStory(story.id)
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error cleaning up stories: ${error.message}")
            }
        })
    }
}
