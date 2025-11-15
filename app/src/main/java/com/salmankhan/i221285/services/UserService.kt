package com.salmankhan.i221285.services

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Service class for managing User Profile operations in Firebase Realtime Database
 */
object UserService {
    private const val TAG = "UserService"
    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")

    /**
     * Update user profile
     */
    suspend fun updateUserProfile(
        userId: String,
        username: String? = null,
        firstName: String? = null,
        lastName: String? = null,
        bio: String? = null,
        website: String? = null,
        email: String? = null,
        phone: String? = null,
        gender: String? = null,
        profileImage: String? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()
            
            username?.let { updates["username"] = it }
            firstName?.let { updates["firstName"] = it }
            lastName?.let { updates["lastName"] = it }
            bio?.let { updates["bio"] = it }
            website?.let { updates["website"] = it }
            email?.let { updates["email"] = it }
            phone?.let { updates["phone"] = it }
            gender?.let { updates["gender"] = it }
            profileImage?.let { updates["profileImage"] = it }
            
            if (updates.isNotEmpty()) {
                usersRef.child(userId).updateChildren(updates).await()
                Log.d(TAG, "User profile updated for: $userId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get user profile data
     */
    suspend fun getUserProfile(userId: String): Result<Map<String, Any?>> {
        return try {
            val snapshot = usersRef.child(userId).get().await()
            
            if (snapshot.exists()) {
                val profileData = mapOf(
                    "username" to snapshot.child("username").getValue(String::class.java),
                    "firstName" to snapshot.child("firstName").getValue(String::class.java),
                    "lastName" to snapshot.child("lastName").getValue(String::class.java),
                    "bio" to snapshot.child("bio").getValue(String::class.java),
                    "website" to snapshot.child("website").getValue(String::class.java),
                    "email" to snapshot.child("email").getValue(String::class.java),
                    "phone" to snapshot.child("phone").getValue(String::class.java),
                    "gender" to snapshot.child("gender").getValue(String::class.java),
                    "profileImage" to snapshot.child("profileImage").getValue(String::class.java),
                    "postsCount" to snapshot.child("postsCount").getValue(Int::class.java),
                    "followersCount" to snapshot.child("followersCount").getValue(Int::class.java),
                    "followingCount" to snapshot.child("followingCount").getValue(Int::class.java)
                )
                Result.success(profileData)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile: ${e.message}", e)
            Result.failure(e)
        }
    }
}

