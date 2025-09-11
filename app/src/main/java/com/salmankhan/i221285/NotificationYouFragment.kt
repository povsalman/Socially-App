package com.salmankhan.i221285

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction

class NotificationYouFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_notification_you, container, false)

        // Find the You Tab TextView and set click listener
        val followingTabTextView = view.findViewById<TextView>(R.id.tab_following)
        followingTabTextView.setOnClickListener {
            // Replace with NotificationYouFragment
            val transaction: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, NotificationFollowerFragment())
            transaction.addToBackStack(null) // Optional: Allows back navigation
            transaction.commit()
        }

        return view
    }
}