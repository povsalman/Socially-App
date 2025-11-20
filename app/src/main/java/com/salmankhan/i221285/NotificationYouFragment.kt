package com.salmankhan.i221285

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.salmankhan.i221285.SociallyApplication
import com.salmankhan.i221285.models.Notification
import com.salmankhan.i221285.repository.NotificationRepository
import kotlinx.coroutines.launch

class NotificationYouFragment : Fragment() {

    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private val notifications = mutableListOf<Notification>()
    
    private val currentUserId: String? by lazy { AuthService.currentUser()?.uid }
    private val notificationRepository by lazy { NotificationRepository(SociallyApplication.getInstance()) }

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
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = notificationRepository.getNotifications()
                if (result.isSuccess) {
                    val fetchedNotifications = result.getOrNull() ?: emptyList()
                    notifications.clear()
                    notifications.addAll(fetchedNotifications)
                    notifications.sortByDescending { it.timestamp }
                    
                    notificationsRecyclerView.adapter?.notifyDataSetChanged()
                    
                    if (notifications.isEmpty()) {
                        notificationsRecyclerView.visibility = View.GONE
                        emptyState.visibility = View.VISIBLE
                    } else {
                        notificationsRecyclerView.visibility = View.VISIBLE
                        emptyState.visibility = View.GONE
                    }
                    
                    Log.d("NotificationYouFragment", "Loaded ${notifications.size} notifications")
                } else {
                    Log.e("NotificationYouFragment", "Error loading notifications: ${result.exceptionOrNull()?.message}")
                    Toast.makeText(context, "Error loading notifications", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("NotificationYouFragment", "Error loading notifications: ${e.message}", e)
                Toast.makeText(context, "Error loading notifications", Toast.LENGTH_SHORT).show()
            }
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