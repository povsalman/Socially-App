package com.salmankhan.i221285.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.salmankhan.i221285.R
import com.salmankhan.i221285.StoryService
import com.salmankhan.i221285.models.Post

/**
 * Adapter for displaying user's posts in a grid layout (3 columns)
 */
class ProfilePostsAdapter(
    private val posts: List<Post>,
    private val onPostClick: (Post) -> Unit
) : RecyclerView.Adapter<ProfilePostsAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.post_image)

        fun bind(post: Post) {
            // Set post image
            if (post.imageBase64.isNotEmpty()) {
                val bitmap: Bitmap? = StoryService.base64ToBitmap(post.imageBase64)
                if (bitmap != null) {
                    postImage.setImageBitmap(bitmap)
                } else {
                    postImage.setImageResource(R.drawable.exp16) // fallback
                }
            } else {
                postImage.setImageResource(R.drawable.exp16) // fallback
            }

            // Handle click
            itemView.setOnClickListener {
                onPostClick(post)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size
}

