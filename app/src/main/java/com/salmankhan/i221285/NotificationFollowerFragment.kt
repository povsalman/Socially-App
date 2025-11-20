package com.salmankhan.i221285

import android.content.Intent
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
import com.salmankhan.i221285.adapters.FollowRequestsAdapter
import com.salmankhan.i221285.models.FollowRequest
import com.salmankhan.i221285.services.FollowService
import kotlinx.coroutines.launch

class NotificationFollowerFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: FollowRequestsAdapter
    private val followRequests = mutableListOf<FollowRequest>()
    
    private val currentUserId: String? by lazy { AuthService.currentUser()?.uid }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification_follower, container, false)

        // Initialize views
        recyclerView = view.findViewById(R.id.follow_requests_recycler_view)
        emptyState = view.findViewById(R.id.empty_state)

        // Setup RecyclerView
        setupRecyclerView()

        // Load follow requests
        loadFollowRequests()

        // Find the You Tab TextView and set click listener
        val youTabTextView = view.findViewById<TextView>(R.id.tab_you)
        youTabTextView.setOnClickListener {
            // Replace with NotificationYouFragment
            val transaction: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, NotificationYouFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        return view
    }

    private fun setupRecyclerView() {
        adapter = FollowRequestsAdapter(
            requests = followRequests,
            onAccept = { request -> acceptFollowRequest(request) },
            onReject = { request -> rejectFollowRequest(request) },
            onUserClick = { request -> openUserProfile(request.fromUserId) }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@NotificationFollowerFragment.adapter
        }
    }

    private fun loadFollowRequests() {
        val userId = currentUserId ?: return
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = FollowService.getPendingFollowRequests(userId)
                if (result.isSuccess) {
                    val requests = result.getOrNull() ?: emptyList()
                    followRequests.clear()
                    followRequests.addAll(requests)
                    adapter.notifyDataSetChanged()
                    
                    if (requests.isEmpty()) {
                        recyclerView.visibility = View.GONE
                        emptyState.visibility = View.VISIBLE
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        emptyState.visibility = View.GONE
                    }
                    
                    Log.d("NotificationFollower", "Loaded ${requests.size} follow requests")
                } else {
                    Log.e("NotificationFollower", "Error loading follow requests: ${result.exceptionOrNull()?.message}")
                    Toast.makeText(context, "Error loading follow requests", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("NotificationFollower", "Error loading follow requests: ${e.message}", e)
                Toast.makeText(context, "Error loading follow requests", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun acceptFollowRequest(request: FollowRequest) {
        lifecycleScope.launch {
            try {
                val userId = currentUserId ?: return@launch
                val result = FollowService.acceptFollowRequest(userId, request.fromUserId)
                
                if (result.isSuccess) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Request accepted!", Toast.LENGTH_SHORT).show()
                        // Reload requests
                        loadFollowRequests()
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationFollower", "Error accepting request: ${e.message}", e)
                activity?.runOnUiThread {
                    Toast.makeText(context, "Error accepting request", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun rejectFollowRequest(request: FollowRequest) {
        lifecycleScope.launch {
            try {
                val userId = currentUserId ?: return@launch
                val result = FollowService.rejectFollowRequest(userId, request.fromUserId)
                
                if (result.isSuccess) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Request rejected", Toast.LENGTH_SHORT).show()
                        // Reload requests
                        loadFollowRequests()
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationFollower", "Error rejecting request: ${e.message}", e)
                activity?.runOnUiThread {
                    Toast.makeText(context, "Error rejecting request", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openUserProfile(userId: String) {
        val intent = Intent(activity, HomeActivity::class.java)
        intent.putExtra("fragment_to_load", "OtherFollowingProfile")
        intent.putExtra("other_user_id", userId)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("force_fragment_load", true)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Reload requests when fragment becomes visible
        loadFollowRequests()
    }
}