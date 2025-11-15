package com.salmankhan.i221285

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Adapter for displaying stories in horizontal RecyclerView
 * Shows user profile images with story borders
 */
class StoriesAdapter(
    private val context: Context,
    private val stories: List<Story>,
    private val onStoryClick: (Story) -> Unit
) : RecyclerView.Adapter<StoriesAdapter.StoryViewHolder>() {
    
    class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val storyImage: CircleImageView = itemView.findViewById(R.id.story_image)
        val usernameText: TextView = itemView.findViewById(R.id.story_username)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]
        val currentUserId = com.salmankhan.i221285.AuthService.currentUser()?.uid
        val isOwnStory = story.userId == currentUserId
        
        // Determine image to display in story bubble
        val imageSource = when {
            !story.userProfileImage.isNullOrBlank() && story.userProfileImage != "default" -> story.userProfileImage
            !story.imageBase64.isNullOrBlank() -> story.imageBase64
            else -> null
        }
        
        // Set image (prefer profile image, fallback to story image, then placeholder)
        val bitmap = imageSource?.let { StoryService.base64ToBitmap(it) }
        if (bitmap != null) {
            holder.storyImage.setImageBitmap(bitmap)
        } else {
            // Fallback to default image
            holder.storyImage.setImageResource(R.drawable.person1)
        }
        
        // Set username - "Your Story" for own story, username for others
        holder.usernameText.text = if (isOwnStory) "Your Story" else story.username
        
        // Set story border (gradient effect) - different color for own story
        if (isOwnStory) {
            holder.storyImage.borderColor = context.getColor(android.R.color.holo_blue_light)
            holder.storyImage.borderWidth = 4
        } else {
            holder.storyImage.borderColor = context.getColor(R.color.story_border_start)
            holder.storyImage.borderWidth = 3
        }
        
        // Handle click
        holder.itemView.setOnClickListener {
            onStoryClick(story)
        }
    }
    
    override fun getItemCount(): Int = stories.size
}
