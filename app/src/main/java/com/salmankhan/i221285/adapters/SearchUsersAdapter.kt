package com.salmankhan.i221285.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.salmankhan.i221285.R
import com.salmankhan.i221285.StoryService
import com.salmankhan.i221285.models.User
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Adapter for displaying users in search results
 */
class SearchUsersAdapter(
    private val users: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<SearchUsersAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.search_user_profile_image)
        val username: TextView = itemView.findViewById(R.id.search_user_username)
        val fullName: TextView = itemView.findViewById(R.id.search_user_full_name)

        fun bind(user: User) {
            username.text = user.username
            fullName.text = if (user.getFullName().isNotEmpty()) user.getFullName() else "No name"

            // Set profile picture
            if (user.profileImage.isNotEmpty()) {
                val bitmap = StoryService.base64ToBitmap(user.profileImage)
                if (bitmap != null) {
                    profileImage.setImageBitmap(bitmap)
                } else {
                    profileImage.setImageResource(R.drawable.jacob_img)
                }
            } else {
                profileImage.setImageResource(R.drawable.jacob_img)
            }

            // Handle click
            itemView.setOnClickListener {
                onUserClick(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size
}

