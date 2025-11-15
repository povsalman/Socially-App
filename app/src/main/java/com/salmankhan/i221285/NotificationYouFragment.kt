package com.salmankhan.i221285

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.salmankhan.i221285.models.Notification
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*

class NotificationYouFragment : Fragment() {

    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private val notifications = mutableListOf<Notification>()
    private var notificationListener: ValueEventListener? = null
    
    private val currentUserId: String? by lazy { AuthService.currentUser()?.uid }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_notification_you, container, false)

        // Find the You Tab TextView and set click listener
        val followingTabTextView = view.findViewById<TextView>(R.id.tab_following)
        followingTabTextView.setOnClickListener {
            // Replace with NotificationFollowerFragment
            val transaction: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, NotificationFollowerFragment())
            transaction.addToBackStack(null) // Optional: Allows back navigation
            transaction.commit()
        }
        
        // Setup RecyclerView for notifications
        setupNotificationsRecyclerView(view)
        
        // Load notifications
        loadNotifications()

        return view
    }
    
    private fun setupNotificationsRecyclerView(view: View) {
        // Find or create RecyclerView in layout
        notificationsRecyclerView = view.findViewById(R.id.notifications_recycler_view) 
            ?: RecyclerView(requireContext()).apply {
                id = R.id.notifications_recycler_view
            }
        
        emptyState = view.findViewById(R.id.empty_state_notifications) 
            ?: LinearLayout(requireContext()).apply {
                id = R.id.empty_state_notifications
            }
        
        notificationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        notificationsRecyclerView.adapter = NotificationsAdapter(notifications)
    }
    
    private fun loadNotifications() {
        val userId = currentUserId ?: return
        
        val notificationsRef = com.google.firebase.database.FirebaseDatabase.getInstance()
            .getReference("notifications")
            .child(userId)
        
        notificationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notifications.clear()
                
                for (child in snapshot.children) {
                    try {
                        val notification = Notification(
                            id = child.key ?: "",
                            type = child.child("type").getValue(String::class.java) ?: "",
                            title = child.child("title").getValue(String::class.java) ?: "",
                            body = child.child("body").getValue(String::class.java) ?: "",
                            timestamp = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis(),
                            read = child.child("read").getValue(Boolean::class.java) ?: false
                        )
                        notifications.add(notification)
                    } catch (e: Exception) {
                        Log.e("NotificationYouFragment", "Error parsing notification: ${e.message}", e)
                    }
                }
                
                // Sort by timestamp (newest first)
                notifications.sortByDescending { it.timestamp }
                
                activity?.runOnUiThread {
                    notificationsRecyclerView.adapter?.notifyDataSetChanged()
                    
                    // Show/hide empty state
                    if (notifications.isEmpty()) {
                        notificationsRecyclerView.visibility = View.GONE
                        emptyState.visibility = View.VISIBLE
                    } else {
                        notificationsRecyclerView.visibility = View.VISIBLE
                        emptyState.visibility = View.GONE
                    }
                    
                    Log.d("NotificationYouFragment", "Loaded ${notifications.size} notifications")
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationYouFragment", "Error loading notifications: ${error.message}")
            }
        }
        
        notificationsRef.addValueEventListener(notificationListener!!)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Remove listener
        val userId = currentUserId ?: return
        notificationListener?.let { listener ->
            com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(userId)
                .removeEventListener(listener)
        }
    }
    
    // Simple adapter for notifications
    private inner class NotificationsAdapter(private val items: List<Notification>) : 
        RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {
        
        inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val titleText: TextView = itemView.findViewById(R.id.notification_title)
            val bodyText: TextView = itemView.findViewById(R.id.notification_body)
            val timeText: TextView = itemView.findViewById(R.id.notification_time)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
            // Create a simple notification item view
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return NotificationViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            val notification = items[position]
            holder.titleText.text = notification.title
            holder.bodyText.text = notification.body
            holder.timeText.text = notification.getTimeAgo()
        }
        
        override fun getItemCount() = items.size
    }
}