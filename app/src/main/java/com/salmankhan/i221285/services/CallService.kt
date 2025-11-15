package com.salmankhan.i221285.services

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Service for managing video call requests and notifications
 */
object CallService {
    private const val TAG = "CallService"
    private val database = FirebaseDatabase.getInstance()
    private val callRequestsRef = database.getReference("callRequests")
    
    /**
     * Send a call request to another user
     */
    suspend fun sendCallRequest(
        callerId: String,
        callerUsername: String,
        receiverId: String,
        chatId: String
    ): Result<String> {
        return try {
            val callRequestId = callRequestsRef.push().key 
                ?: return Result.failure(Exception("Failed to generate call request ID"))
            
            val callRequest = mapOf(
                "id" to callRequestId,
                "callerId" to callerId,
                "callerUsername" to callerUsername,
                "receiverId" to receiverId,
                "chatId" to chatId,
                "status" to "pending", // pending, accepted, declined, missed
                "timestamp" to System.currentTimeMillis()
            )
            
            // Store under receiver's ID for easy lookup
            callRequestsRef.child(receiverId).child(callRequestId).setValue(callRequest).await()
            
            Log.d(TAG, "Call request sent: $callerId -> $receiverId")
            Result.success(callRequestId)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending call request: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Accept a call request
     */
    suspend fun acceptCallRequest(receiverId: String, callRequestId: String): Result<Unit> {
        return try {
            callRequestsRef.child(receiverId).child(callRequestId)
                .child("status").setValue("accepted").await()
            
            Log.d(TAG, "Call request accepted: $callRequestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting call request: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Decline a call request
     */
    suspend fun declineCallRequest(receiverId: String, callRequestId: String): Result<Unit> {
        return try {
            callRequestsRef.child(receiverId).child(callRequestId)
                .child("status").setValue("declined").await()
            
            // Remove after 5 seconds
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                callRequestsRef.child(receiverId).child(callRequestId).removeValue()
            }, 5000)
            
            Log.d(TAG, "Call request declined: $callRequestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error declining call request: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Cancel a call request (caller side)
     */
    suspend fun cancelCallRequest(receiverId: String, callRequestId: String): Result<Unit> {
        return try {
            callRequestsRef.child(receiverId).child(callRequestId).removeValue().await()
            
            Log.d(TAG, "Call request cancelled: $callRequestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling call request: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Mark call as missed if not answered in time
     */
    suspend fun markCallAsMissed(receiverId: String, callRequestId: String): Result<Unit> {
        return try {
            callRequestsRef.child(receiverId).child(callRequestId)
                .child("status").setValue("missed").await()
            
            // Remove after a delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                callRequestsRef.child(receiverId).child(callRequestId).removeValue()
            }, 10000)
            
            Log.d(TAG, "Call marked as missed: $callRequestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking call as missed: ${e.message}", e)
            Result.failure(e)
        }
    }
}

