package com.salmankhan.i221285.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.salmankhan.i221285.R
import com.salmankhan.i221285.StoryService
import com.salmankhan.i221285.models.Post
import de.hdodenhof.circleimageview.CircleImageView

class PostsAdapter(
    private val context: Context,
    private val posts: MutableList<Post>,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onShareClick: (Post) -> Unit,
    private val onSaveClick: (Post) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post_dynamic, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post)
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: CircleImageView = itemView.findViewById(R.id.post_user_profile_image)
        private val username: TextView = itemView.findViewById(R.id.post_username)
        private val postImage: ImageView = itemView.findViewById(R.id.post_image)
        private val likeButton: ImageView = itemView.findViewById(R.id.like_button)
        private val commentButton: ImageView = itemView.findViewById(R.id.comment_button)
        private val shareButton: ImageView = itemView.findViewById(R.id.share_button)
        private val saveButton: ImageView = itemView.findViewById(R.id.save_button)
        private val likesCount: TextView = itemView.findViewById(R.id.likes_count)
        private val caption: TextView = itemView.findViewById(R.id.post_caption)
        private val commentsCount: TextView = itemView.findViewById(R.id.comments_count)
        private val timestamp: TextView = itemView.findViewById(R.id.post_timestamp)

        fun bind(post: Post) {
            // Set username
            username.text = post.username

            // Set profile image
            if (post.userProfileImage.isNotEmpty()) {
                val bitmap = StoryService.base64ToBitmap(post.userProfileImage)
                if (bitmap != null) {
                    profileImage.setImageBitmap(bitmap)
                } else {
                    profileImage.setImageResource(R.drawable.default_profile_pic)
                }
            } else {
                profileImage.setImageResource(R.drawable.default_profile_pic)
            }

            // Set post image
            val postBitmap = StoryService.base64ToBitmap(post.imageBase64)
            if (postBitmap != null) {
                postImage.setImageBitmap(postBitmap)
            } else {
                postImage.setImageResource(R.drawable.exp7) // Placeholder
            }

            // Set likes count
            likesCount.text = if (post.likesCount > 0) {
                "${post.likesCount} likes"
            } else {
                "Be the first to like this"
            }

            // Set caption
            if (post.caption.isNotEmpty()) {
                caption.visibility = View.VISIBLE
                caption.text = "${post.username} ${post.caption}"
            } else {
                caption.visibility = View.GONE
            }

            // Set comments count
            if (post.commentsCount > 0) {
                commentsCount.visibility = View.VISIBLE
                commentsCount.text = "View all ${post.commentsCount} comments"
            } else {
                commentsCount.visibility = View.GONE
            }

            // Set timestamp
            timestamp.text = formatTimestamp(post.createdAt)

            // Set click listeners
            likeButton.setOnClickListener {
                onLikeClick(post)
            }

            commentButton.setOnClickListener {
                onCommentClick(post)
            }

            shareButton.setOnClickListener {
                onShareClick(post)
            }

            saveButton.setOnClickListener {
                onSaveClick(post)
            }
            
            commentsCount.setOnClickListener {
                onCommentClick(post)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            
            return when {
                days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
                hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
                minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
                else -> "Just now"
            }
        }
    }
}



