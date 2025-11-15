package com.salmankhan.i221285.services

import android.util.Log
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await

/**
 * Service class for managing Follow/Unfollow system in Firebase Realtime Database
 */
object FollowService {
    private const val TAG = "FollowService"
    private val database = FirebaseDatabase.getInstance()
    private val followingRef = database.getReference("following")
    private val followersRef = database.getReference("followers")
    private val followRequestsRef = database.getReference("followRequests")
    private val usersRef = database.getReference("users")
    
    /**
     * Send a follow request to another user
     */
    suspend fun sendFollowRequest(fromUserId: String, toUserId: String, fromUsername: String, fromUserProfileImage: String): Result<Unit> {
        return try {
            // Check if already following
            if (isFollowing(fromUserId, toUserId)) {
                return Result.failure(Exception("Already following this user"))
            }
            
            // Check if request already exists
            val existingRequest = followRequestsRef.child(toUserId).child(fromUserId).get().await()
            if (existingRequest.exists()) {
                return Result.failure(Exception("Follow request already sent"))
            }
            
            val request = mapOf(
                "fromUserId" to fromUserId,
                "toUserId" to toUserId,
                "fromUsername" to fromUsername,
                "fromUserProfileImage" to fromUserProfileImage,
                "status" to "pending",
                "createdAt" to System.currentTimeMillis()
            )
            
            // Store request under recipient's ID for easy retrieval
            followRequestsRef.child(toUserId).child(fromUserId).setValue(request).await()
            
            // Send push notification
            NotificationHelper.sendFollowRequestNotification(toUserId, fromUsername)
            
            Log.d(TAG, "Follow request sent from $fromUserId to $toUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending follow request: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Accept a follow request
     */
    suspend fun acceptFollowRequest(currentUserId: String, fromUserId: String): Result<Unit> {
        return try {
            // Add to following/followers
            val timestamp = System.currentTimeMillis()
            followingRef.child(fromUserId).child(currentUserId).setValue(timestamp).await()
            followersRef.child(currentUserId).child(fromUserId).setValue(timestamp).await()
            
            // Update counts
            incrementFollowingCount(fromUserId)
            incrementFollowersCount(currentUserId)
            
            // Delete the request
            followRequestsRef.child(currentUserId).child(fromUserId).removeValue().await()
            
            Log.d(TAG, "Follow request accepted: $fromUserId -> $currentUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting follow request: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Reject a follow request
     */
    suspend fun rejectFollowRequest(currentUserId: String, fromUserId: String): Result<Unit> {
        return try {
            // Delete the request
            followRequestsRef.child(currentUserId).child(fromUserId).removeValue().await()
            
            Log.d(TAG, "Follow request rejected: $fromUserId -> $currentUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting follow request: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Cancel a sent follow request
     */
    suspend fun cancelFollowRequest(fromUserId: String, toUserId: String): Result<Unit> {
        return try {
            followRequestsRef.child(toUserId).child(fromUserId).removeValue().await()
            Log.d(TAG, "Follow request cancelled: $fromUserId -> $toUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling follow request: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get pending follow requests for a user
     */
    fun getPendingFollowRequests(userId: String, callback: (List<com.salmankhan.i221285.models.FollowRequest>) -> Unit) {
        followRequestsRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = mutableListOf<com.salmankhan.i221285.models.FollowRequest>()
                for (child in snapshot.children) {
                    try {
                        val request = com.salmankhan.i221285.models.FollowRequest(
                            fromUserId = child.child("fromUserId").getValue(String::class.java) ?: "",
                            toUserId = child.child("toUserId").getValue(String::class.java) ?: "",
                            fromUsername = child.child("fromUsername").getValue(String::class.java) ?: "",
                            fromUserProfileImage = child.child("fromUserProfileImage").getValue(String::class.java) ?: "",
                            status = child.child("status").getValue(String::class.java) ?: "pending",
                            createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L
                        )
                        if (request.status == "pending") {
                            requests.add(request)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing follow request: ${e.message}", e)
                    }
                }
                callback(requests)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error loading follow requests: ${error.message}")
                callback(emptyList())
            }
        })
    }
    
    /**
     * Check if a follow request is pending
     */
    suspend fun hasRequestPending(fromUserId: String, toUserId: String): Boolean {
        return try {
            val snapshot = followRequestsRef.child(toUserId).child(fromUserId).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking pending request: ${e.message}", e)
            false
        }
    }
    
    /**
     * Follow a user (direct follow, no request)
     */
    suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            
            // Add to current user's following list
            followingRef.child(currentUserId).child(targetUserId).setValue(timestamp).await()
            
            // Add to target user's followers list
            followersRef.child(targetUserId).child(currentUserId).setValue(timestamp).await()
            
            // Update counts
            incrementFollowingCount(currentUserId)
            incrementFollowersCount(targetUserId)
            
            Log.d(TAG, "User $currentUserId followed $targetUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error following user: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Unfollow a user
     */
    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit> {
        return try {
            // Remove from current user's following list
            followingRef.child(currentUserId).child(targetUserId).removeValue().await()
            
            // Remove from target user's followers list
            followersRef.child(targetUserId).child(currentUserId).removeValue().await()
            
            // Update counts
            decrementFollowingCount(currentUserId)
            decrementFollowersCount(targetUserId)
            
            Log.d(TAG, "User $currentUserId unfollowed $targetUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unfollowing user: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if user is following another user
     */
    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        return try {
            val snapshot = followingRef.child(currentUserId).child(targetUserId).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking follow status: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get list of users that current user is following
     */
    suspend fun getFollowingList(userId: String): List<String> {
        return try {
            val snapshot = followingRef.child(userId).get().await()
            val followingList = mutableListOf<String>()
            for (child in snapshot.children) {
                followingList.add(child.key ?: continue)
            }
            followingList
        } catch (e: Exception) {
            Log.e(TAG, "Error getting following list: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get list of users that follow the current user
     */
    suspend fun getFollowersList(userId: String): List<String> {
        return try {
            val snapshot = followersRef.child(userId).get().await()
            val followersList = mutableListOf<String>()
            for (child in snapshot.children) {
                followersList.add(child.key ?: continue)
            }
            followersList
        } catch (e: Exception) {
            Log.e(TAG, "Error getting followers list: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get following count
     */
    suspend fun getFollowingCount(userId: String): Int {
        return try {
            val snapshot = followingRef.child(userId).get().await()
            snapshot.childrenCount.toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting following count: ${e.message}", e)
            0
        }
    }
    
    /**
     * Get followers count
     */
    suspend fun getFollowersCount(userId: String): Int {
        return try {
            val snapshot = followersRef.child(userId).get().await()
            snapshot.childrenCount.toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting followers count: ${e.message}", e)
            0
        }
    }
    
    // Helper methods for updating user profile counts
    
    private fun incrementFollowingCount(userId: String) {
        usersRef.child(userId).child("followingCount").runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentValue = currentData.getValue(Int::class.java) ?: 0
                currentData.value = currentValue + 1
                return Transaction.success(currentData)
            }
            
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e(TAG, "Error incrementing following count: ${error.message}")
                }
            }
        })
    }
    
    private fun decrementFollowingCount(userId: String) {
        usersRef.child(userId).child("followingCount").runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentValue = currentData.getValue(Int::class.java) ?: 0
                currentData.value = maxOf(0, currentValue - 1)
                return Transaction.success(currentData)
            }
            
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e(TAG, "Error decrementing following count: ${error.message}")
                }
            }
        })
    }
    
    private fun incrementFollowersCount(userId: String) {
        usersRef.child(userId).child("followersCount").runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentValue = currentData.getValue(Int::class.java) ?: 0
                currentData.value = currentValue + 1
                return Transaction.success(currentData)
            }
            
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e(TAG, "Error incrementing followers count: ${error.message}")
                }
            }
        })
    }
    
    private fun decrementFollowersCount(userId: String) {
        usersRef.child(userId).child("followersCount").runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentValue = currentData.getValue(Int::class.java) ?: 0
                currentData.value = maxOf(0, currentValue - 1)
                return Transaction.success(currentData)
            }
            
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e(TAG, "Error decrementing followers count: ${error.message}")
                }
            }
        })
    }
}

