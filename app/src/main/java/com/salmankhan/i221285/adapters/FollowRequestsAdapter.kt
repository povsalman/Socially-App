package com.salmankhan.i221285.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.salmankhan.i221285.R
import com.salmankhan.i221285.StoryService
import com.salmankhan.i221285.models.FollowRequest
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Adapter for displaying follow requests in notifications
 */
class FollowRequestsAdapter(
    private val requests: List<FollowRequest>,
    private val onAccept: (FollowRequest) -> Unit,
    private val onReject: (FollowRequest) -> Unit,
    private val onUserClick: (FollowRequest) -> Unit
) : RecyclerView.Adapter<FollowRequestsAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.request_user_profile_image)
        val username: TextView = itemView.findViewById(R.id.request_user_username)
        val message: TextView = itemView.findViewById(R.id.request_message)
        val acceptButton: MaterialButton = itemView.findViewById(R.id.accept_button)
        val rejectButton: MaterialButton = itemView.findViewById(R.id.reject_button)

        fun bind(request: FollowRequest) {
            username.text = request.fromUsername
            message.text = "wants to follow you"

            // Set profile picture
            if (request.fromUserProfileImage.isNotEmpty()) {
                val bitmap = StoryService.base64ToBitmap(request.fromUserProfileImage)
                if (bitmap != null) {
                    profileImage.setImageBitmap(bitmap)
                } else {
                    profileImage.setImageResource(R.drawable.jacob_img)
                }
            } else {
                profileImage.setImageResource(R.drawable.jacob_img)
            }

            // Handle clicks
            profileImage.setOnClickListener { onUserClick(request) }
            username.setOnClickListener { onUserClick(request) }
            
            acceptButton.setOnClickListener { onAccept(request) }
            rejectButton.setOnClickListener { onReject(request) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(requests[position])
    }

    override fun getItemCount(): Int = requests.size
}

