package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.ImageView

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Find the dms ImageView and set click listener
        val dmsImageView = view.findViewById<ImageView>(R.id.dms)
        dmsImageView.setOnClickListener {
            val intent = Intent(activity, DmActivity::class.java)
            startActivity(intent)
        }

        // Find the camera ImageView and set click listener
        val cameraImageView = view.findViewById<ImageView>(R.id.camera)
        cameraImageView.setOnClickListener {
            val intent = Intent(activity, StoryTakeActivity::class.java)
            startActivity(intent)
        }


        return view
    }
}