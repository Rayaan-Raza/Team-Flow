package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class project_list : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val dbRef = FirebaseDatabase.getInstance().reference
    private val realtimeListeners = RealtimeListeners()
    
    private lateinit var rvProjects: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var projectsAdapter: ProjectsAdapter
    private val projectList = mutableListOf<Project>()
    
    // SQLite support
    private lateinit var syncManager: SyncManager
    
    // Bottom navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navProjects: LinearLayout
    private lateinit var navCalendar: LinearLayout
    private lateinit var navInbox: LinearLayout
    private lateinit var navProfile: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)  // Reusing home page layout
        
        auth = FirebaseAuth.getInstance()
        syncManager = SyncManager(this)
        
        rvProjects = findViewById(R.id.rvTasks)
        tvEmptyState = findViewById(R.id.tvUpcomingCount)
        
        // Setup RecyclerView
        rvProjects.layoutManager = LinearLayoutManager(this)
        projectsAdapter = ProjectsAdapter(projectList) { project ->
            val pid = project.id ?: return@ProjectsAdapter
            val intent = Intent(this, project_detail::class.java)
            intent.putExtra("projectId", pid)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        rvProjects.adapter = projectsAdapter
        
        val tvHello = findViewById<TextView>(R.id.tvHello)
        val userName = UserSession.getName(this)
        tvHello.text = if (!userName.isNullOrEmpty()) "Hi, $userName" else "Hi,"
        
        // New task button
        val btnNewTask = findViewById<android.widget.Button>(R.id.btnNewTask)
        btnNewTask.setOnClickListener {
            startActivity(Intent(this, add_edit_project::class.java))
            overridePendingTransition(0, 0)
        }
        
        // Setup bottom navigation
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.NavItem.PROJECTS)
    }
    
    override fun onStart() {
        super.onStart()
        
        // Check authentication
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Sign_in::class.java))
            finish()
            return
        }
        
        loadProjects(currentUser.uid)
    }
    
    private fun loadProjects(uid: String) {
        if (NetworkUtils.isInternetAvailable(this)) {
            // Online: load from Firebase and cache
            realtimeListeners.attachProjectsListener(uid) { projects ->
                projectList.clear()
                projectList.addAll(projects)
                projectsAdapter.notifyDataSetChanged()
                
                // Cache to SQLite
                syncManager.cacheProjects(projects)
                
                if (projects.isEmpty()) {
                    tvEmptyState.text = "No projects yet"
                } else {
                    tvEmptyState.text = "${projects.size} Projects"
                }
            }
        } else {
            // Offline: load from cache
            val cachedProjects = syncManager.getCachedProjects()
            projectList.clear()
            projectList.addAll(cachedProjects)
            projectsAdapter.notifyDataSetChanged()
            
            if (cachedProjects.isEmpty()) {
                tvEmptyState.text = "No offline data"
            } else {
                tvEmptyState.text = "${cachedProjects.size} Projects (offline)"
                Toast.makeText(this, "Loaded ${cachedProjects.size} projects offline", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onStop() {
        super.onStop()
        realtimeListeners.detachAllListeners()
    }
}
