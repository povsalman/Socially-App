package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView

class SelfProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_self_profile, container, false)

        val editProfileButton = view.findViewById<MaterialButton>(R.id.edit_profile_button)
        editProfileButton.setOnClickListener {
            // Handle edit profile button click
            val intent = Intent(activity, EditProfileActivity::class.java)
            startActivity(intent)
        }

        val highlightImage1 = view.findViewById<CircleImageView>(R.id.highlight_image_1)
        highlightImage1.setOnClickListener {
            // Handle highlight image click
            val intent = Intent(activity, ViewHighlightActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}