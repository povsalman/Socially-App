package com.salmankhan.i221285

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.salmankhan.i221285.adapters.ChatsAdapter
import com.salmankhan.i221285.models.Chat
import com.salmankhan.i221285.repository.UserRepository
import com.salmankhan.i221285.services.MessageService
import com.salmankhan.i221285.SociallyApplication
import kotlinx.coroutines.launch

class DmActivity : AppCompatActivity() {

    private lateinit var chatsRecyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var chatsAdapter: ChatsAdapter
    private val chats = mutableListOf<Chat>()
    
    private val currentUserId: String? by lazy { AuthService.currentUser()?.uid }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dm)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        chatsRecyclerView = findViewById(R.id.chats_recycler_view)
        emptyState = findViewById(R.id.empty_state)
        
        // Set current user's username in header
        setHeaderUsername()

        // Setup RecyclerView
        setupRecyclerView()

        // Load chats
        loadChats()

        // Bottom camera button click → StoryTakeActivity
        val bottomCameraButton = findViewById<MaterialButton>(R.id.bottom_camera)
        bottomCameraButton.setOnClickListener {
            val intent = Intent(this, StoryTakeActivity::class.java)
            startActivity(intent)
        }

        // Back arrow → Previous screen
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        backIcon.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        val userId = currentUserId ?: return
        
        chatsAdapter = ChatsAdapter(chats, userId) { chat ->
            openChat(chat)
        }
        
        chatsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DmActivity)
            adapter = chatsAdapter
        }
    }

    private fun loadChats() {
        val userId = currentUserId ?: return
        
        MessageService.getUserChats(userId) { chatsList ->
            runOnUiThread {
                chats.clear()
                chats.addAll(chatsList)
                chatsAdapter.notifyDataSetChanged()
                
                if (chatsList.isEmpty()) {
                    chatsRecyclerView.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                } else {
                    chatsRecyclerView.visibility = View.VISIBLE
                    emptyState.visibility = View.GONE
                }
                
                Log.d("DmActivity", "Loaded ${chatsList.size} chats")
            }
        }
    }

    private fun openChat(chat: Chat) {
        val userId = currentUserId ?: return
        
        // Determine the other user
        val otherUserId = if (userId == chat.user1Id) chat.user2Id else chat.user1Id
        val otherUsername = if (userId == chat.user1Id) chat.user2Name else chat.user1Name
        
        val intent = Intent(this, PersonaldmActivity::class.java)
        intent.putExtra("chat_id", chat.id)
        intent.putExtra("other_user_id", otherUserId)
        intent.putExtra("other_username", otherUsername)
        startActivity(intent)
    }
    
    private fun setHeaderUsername() {
        val userId = currentUserId ?: return
        val headerTextView = findViewById<TextView>(R.id.dm_header_username)
        
        lifecycleScope.launch {
            try {
                val userRepository = UserRepository(SociallyApplication.getInstance())
                val result = userRepository.getUserProfile(userId.toIntOrNull())
                val username = if (result.isSuccess) {
                    result.getOrNull()?.username
                } else null
                
                runOnUiThread {
                    if (!username.isNullOrEmpty() && username != "jacob_w") {
                        headerTextView.text = username
                    } else {
                        headerTextView.text = "Direct Messages"
                    }
                }
            } catch (e: Exception) {
                Log.e("DmActivity", "Error loading username: ${e.message}", e)
                runOnUiThread {
                    headerTextView.text = "Direct Messages"
                }
            }
        }
    }
}
