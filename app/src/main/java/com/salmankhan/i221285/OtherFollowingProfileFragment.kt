package com.salmankhan.i221285

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class OtherFollowingProfileFragment : Fragment() {

    private lateinit var followButton: MaterialButton
    private var isFollowing: Boolean = true // Track the current state

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_other_following_profile, container, false)

        // Initialize the follow button
        followButton = view.findViewById(R.id.followButton)

        // Set initial state
        updateFollowButton()

        // Set click listener to toggle state
        followButton.setOnClickListener {
            isFollowing = !isFollowing
            updateFollowButton()
        }

        // Back arrow → Previous screen
        val backIcon = view.findViewById<ImageView>(R.id.back_icon)
        backIcon.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        return view
    }

    private fun updateFollowButton() {
        if (isFollowing) {
            followButton.text = getString(R.string.following_button)
            followButton.backgroundTintList = null // Default style
            followButton.setTextColor(requireContext().getColor(R.color.black))
        } else {
            followButton.text = getString(R.string.follow_button)
            followButton.backgroundTintList = requireContext().getColorStateList(R.color.text_color)
            followButton.setTextColor(requireContext().getColor(R.color.white))
        }
    }
}
