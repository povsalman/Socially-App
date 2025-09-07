package com.salmankhan.i221285

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.salmankhan.i221285.PostAdapter
import com.salmankhan.i221285.Post

class HomeActivity : AppCompatActivity() {
    lateinit var recyclerView: RecyclerView
    lateinit var postAdapter: PostAdapter
    val postList = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        recyclerView = findViewById(R.id.recyclerViewPosts)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Sample posts (INTEGRATE API CALLS TO FETCH REAL DATA LATER)
        postList.add(
            Post("karennne", "Tokyo, Japan", R.drawable.person1, R.drawable.homepage_post, "Enjoying my trip ✈️")
        )
        postList.add(
            Post("john_doe", "New York, USA", R.drawable.person2, R.drawable.homepage_post2, "Sunset vibes 🌇")
        )
        postList.add(
            Post("sophia", "Paris, France", R.drawable.person3, R.drawable.homepage_post3, "Coffee time ☕")
        )

        postAdapter = PostAdapter(postList)
        recyclerView.adapter = postAdapter
    }
}