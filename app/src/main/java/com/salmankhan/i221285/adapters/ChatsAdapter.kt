package com.salmankhan.i221285.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.salmankhan.i221285.R
import com.salmankhan.i221285.StoryService
import com.salmankhan.i221285.models.Chat
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying chats in DM list
 */
class ChatsAdapter(
    private val chats: List<Chat>,
    private val currentUserId: String,
    private val onChatClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatsAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.chat_user_profile_image)
        val username: TextView = itemView.findViewById(R.id.chat_username)
        val lastMessage: TextView = itemView.findViewById(R.id.chat_last_message)
        val timestamp: TextView = itemView.findViewById(R.id.chat_timestamp)
        val unreadBadge: TextView = itemView.findViewById(R.id.unread_badge)

        fun bind(chat: Chat) {
            // Determine which user is the "other" user
            val isUser1 = currentUserId == chat.user1Id
            val otherUserId = if (isUser1) chat.user2Id else chat.user1Id
            var otherUsername = if (isUser1) chat.user2Name else chat.user1Name
            val otherProfileImage = if (isUser1) chat.user2ProfileImage else chat.user1ProfileImage
            val unreadCount = if (isUser1) chat.unreadCount_user1 else chat.unreadCount_user2

            // Set username initially
            username.text = otherUsername
            
            // Fix: If username is empty, "jacob_w", or "User", fetch from Firebase
            if (otherUsername.isEmpty() || 
                otherUsername == "jacob_w" || 
                otherUsername == "User" || 
                otherUsername == "username") {
                // Fetch username from Firebase
                fetchUsernameFromFirebase(otherUserId) { fetchedUsername ->
                    // Update the username text view with fetched username
                    username.text = fetchedUsername
                }
            }

            // Set profile picture
            if (otherProfileImage.isNotEmpty()) {
                val bitmap = StoryService.base64ToBitmap(otherProfileImage)
                if (bitmap != null) {
                    profileImage.setImageBitmap(bitmap)
                } else {
                    profileImage.setImageResource(R.drawable.jacob_img)
                }
            } else {
                profileImage.setImageResource(R.drawable.jacob_img)
            }

            // Set last message
            lastMessage.text = if (chat.lastMessage.isEmpty()) "No messages yet" else chat.lastMessage

            // Set timestamp
            timestamp.text = formatTimestamp(chat.lastMessageTime)

            // Show/hide unread badge
            if (unreadCount > 0) {
                unreadBadge.visibility = View.VISIBLE
                unreadBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
            } else {
                unreadBadge.visibility = View.GONE
            }

            // Handle click
            itemView.setOnClickListener {
                onChatClick(chat)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60 * 1000 -> "Just now"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
                diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int = chats.size
    
    private fun fetchUsernameFromFirebase(userId: String, callback: (String) -> Unit) {
        val userRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("username")
        
        userRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val fetchedUsername = snapshot.getValue(String::class.java) ?: "User"
                callback(fetchedUsername)
                Log.d("ChatsAdapter", "Fetched username for $userId: $fetchedUsername")
            }
            
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("ChatsAdapter", "Error fetching username: ${error.message}")
                callback("User")
            }
        })
    }
}

