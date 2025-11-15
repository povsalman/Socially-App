package com.salmankhan.i221285.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.salmankhan.i221285.R
import com.salmankhan.i221285.StoryService
import com.salmankhan.i221285.models.Comment
import de.hdodenhof.circleimageview.CircleImageView

class CommentsAdapter(
    private val context: Context,
    private val comments: List<Comment>
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.bind(comment)
    }

    override fun getItemCount(): Int = comments.size

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: CircleImageView = itemView.findViewById(R.id.comment_user_profile_image)
        private val username: TextView = itemView.findViewById(R.id.comment_username)
        private val commentText: TextView = itemView.findViewById(R.id.comment_text)
        private val timestamp: TextView = itemView.findViewById(R.id.comment_timestamp)

        fun bind(comment: Comment) {
            // Set username
            username.text = comment.username

            // Set profile image
            if (comment.userProfileImage.isNotEmpty()) {
                val bitmap = StoryService.base64ToBitmap(comment.userProfileImage)
                if (bitmap != null) {
                    profileImage.setImageBitmap(bitmap)
                } else {
                    profileImage.setImageResource(R.drawable.default_profile_pic)
                }
            } else {
                profileImage.setImageResource(R.drawable.default_profile_pic)
            }

            // Set comment text
            commentText.text = comment.text

            // Set timestamp
            timestamp.text = formatTimestamp(comment.createdAt)
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            val weeks = days / 7

            return when {
                weeks > 0 -> "${weeks}w"
                days > 0 -> "${days}d"
                hours > 0 -> "${hours}h"
                minutes > 0 -> "${minutes}m"
                else -> "now"
            }
        }
    }
}

