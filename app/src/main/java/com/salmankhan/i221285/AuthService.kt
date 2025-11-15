package com.salmankhan.i221285

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AuthService {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    fun currentUser(): FirebaseUser? = auth.currentUser

    suspend fun signUpWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
    }

    suspend fun signInWithEmail(email: String, password: String) {
        try {
            android.util.Log.d("AuthService", "Starting sign in for: $email")
            android.util.Log.d("AuthService", "Creating Firebase auth task")
            
            val authTask = auth.signInWithEmailAndPassword(email, password)
            android.util.Log.d("AuthService", "Auth task created, adding listeners")
            
            // Add completion listener to see if Firebase responds at all
            authTask.addOnCompleteListener { task ->
                android.util.Log.d("AuthService", "OnCompleteListener called")
                if (task.isSuccessful) {
                    android.util.Log.d("AuthService", "Task successful: ${task.result?.user?.uid}")
                } else {
                    android.util.Log.e("AuthService", "Task failed: ${task.exception?.message}")
                }
            }
            
            android.util.Log.d("AuthService", "Waiting for auth task with 30 second timeout...")
            
            // Add timeout to prevent infinite waiting
            val result = withContext(Dispatchers.IO) {
                withTimeout(30000L) { // 30 second timeout
                    authTask.await()
                }
            }
            
            android.util.Log.d("AuthService", "Auth task completed!")
            android.util.Log.d("AuthService", "Sign in successful for user: ${result.user?.uid}")
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            android.util.Log.e("AuthService", "Sign in TIMEOUT after 30 seconds")
            throw Exception("Login timeout - please check your internet connection")
        } catch (e: Exception) {
            android.util.Log.e("AuthService", "Sign in failed: ${e.message}", e)
            throw e
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun initializeUserProfileIfMissing(
        uid: String,
        username: String?,
        firstName: String?,
        lastName: String?
    ) {
        try {
            android.util.Log.d("AuthService", "Creating profile for user: $uid")
            val ref = Firebase.database.getReference("users").child(uid)
            val snapshot = ref.get().await()
            if (!snapshot.exists()) {
                val profile = mapOf(
                    "username" to (username ?: ""),
                    "firstName" to (firstName ?: ""),
                    "lastName" to (lastName ?: ""),
                    "profileCompleted" to false,
                    "createdAt" to System.currentTimeMillis()
                )
                ref.setValue(profile).await()
                android.util.Log.d("AuthService", "Profile created successfully for: $uid")
            } else {
                android.util.Log.d("AuthService", "Profile already exists for: $uid")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthService", "Error creating profile: ${e.message}", e)
            throw e
        }
    }

    suspend fun isProfileCompleted(uid: String): Boolean {
        return try {
            val ref = Firebase.database.getReference("users").child(uid).child("profileCompleted")
            val snapshot = ref.get().await()
            val result = snapshot.getValue(Boolean::class.java) == true
            android.util.Log.d("AuthService", "Profile completed check for $uid: $result")
            result
        } catch (e: Exception) {
            android.util.Log.e("AuthService", "Error checking profile completion: ${e.message}", e)
            false // Default to not completed if there's an error
        }
    }
}




