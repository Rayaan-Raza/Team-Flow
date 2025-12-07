package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class home_page : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val dbRef = FirebaseDatabase.getInstance().reference

    private lateinit var tvHello: TextView
    private lateinit var tvUpcomingCount: TextView
    private lateinit var btnNewTask: Button
    private lateinit var rvTasks: RecyclerView

    private lateinit var navHome: LinearLayout
    private lateinit var navProjects: LinearLayout
    private lateinit var navCalendar: LinearLayout
    private lateinit var navInbox: LinearLayout
    private lateinit var navProfile: LinearLayout

    private lateinit var projectsAdapter: ProjectsAdapter
    private val projectList = mutableListOf<Project>()
    private val realtimeListeners = RealtimeListeners()
    private var projectsListenerKey: String? = null
    
    // SQLite sync manager
    private lateinit var syncManager: SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        auth = FirebaseAuth.getInstance()
        syncManager = SyncManager(this)

        tvHello = findViewById(R.id.tvHello)
        tvUpcomingCount = findViewById(R.id.tvUpcomingCount)
        btnNewTask = findViewById(R.id.btnNewTask)
        rvTasks = findViewById(R.id.rvTasks)
        
        // Notification bell button
        val ivBell = findViewById<android.widget.ImageView>(R.id.ivBell)
        ivBell.setOnClickListener {
            startActivity(Intent(this, notifications_fill::class.java))
            overridePendingTransition(0, 0)
        }

        // Initialize RecyclerView and adapter
        rvTasks.layoutManager = LinearLayoutManager(this)
        projectsAdapter = ProjectsAdapter(projectList) { project ->
            val pid = project.id ?: return@ProjectsAdapter
            val intent = Intent(this, project_detail::class.java)
            intent.putExtra("projectId", pid)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        rvTasks.adapter = projectsAdapter

        btnNewTask.setOnClickListener {
            startActivity(Intent(this, add_edit_project::class.java))
            overridePendingTransition(0, 0)
        }

        // Setup bottom navigation
        bottomNav()
    }


    fun bottomNav(){
        navHome = findViewById(R.id.navHome)
        navProjects = findViewById(R.id.navProjects)
        navCalendar = findViewById(R.id.navCalendar)
        navInbox = findViewById(R.id.navInbox)
        navProfile = findViewById(R.id.navProfile)

        navCalendar.setOnClickListener { 
            startActivity(Intent(this, home_page::class.java))
            overridePendingTransition(0,0)
            finish()
        }
        
        navInbox.setOnClickListener { 
            startActivity(Intent(this, inbox_all::class.java))
            overridePendingTransition(0,0)
            finish()
        }
        
        navProfile.setOnClickListener{
            startActivity(Intent(this, profile_screen::class.java))
            overridePendingTransition(0,0)
            finish()
        }

        navHome.setOnClickListener {
            // Already on home, do nothing
        }

        navProjects.setOnClickListener {
            startActivity(Intent(this, project_list::class.java))
            overridePendingTransition(0,0)
            finish()
        }

    }

    override fun onResume() {
        super.onResume()
        loadProjects()
    }

    override fun onPause() {
        super.onPause()
        projectsListenerKey?.let { realtimeListeners.detachListener(it) }
    }


    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Sign_in::class.java))
            overridePendingTransition(0, 0)
            finish()
            return
        }
        
        // Load user name from UserSession
        val userName = UserSession.getName(this)
        tvHello.text = if (!userName.isNullOrEmpty()) "Hi, $userName" else "Hi,"
        
        // Load user name from Firebase if not in session and online
        if (userName.isNullOrEmpty() && NetworkUtils.isInternetAvailable(this)) {
            loadUserName()
        }
    }

    private fun loadUserName() {
        val uid = auth.currentUser?.uid ?: return
        dbRef.child("users").child(uid).get()
            .addOnSuccessListener { snap ->
                val name = snap.child("name").getValue(String::class.java)
                tvHello.text = if (!name.isNullOrEmpty()) "Hi, $name" else "Hi,"
                // Save to session for offline access
                if (!name.isNullOrEmpty()) {
                    UserSession.saveName(this, name)
                }
            }
            .addOnFailureListener {
                // Silently fail - name already loaded from session if available
            }
    }

    private fun loadProjects() {
        val uid = auth.currentUser?.uid ?: return
        
        if (NetworkUtils.isInternetAvailable(this)) {
            // Online: Load from Firebase and cache to SQLite
            loadProjectsFromFirebase(uid)
        } else {
            // Offline: Load from SQLite cache
            loadProjectsFromCache()
        }
    }
    
    private fun loadProjectsFromFirebase(uid: String) {
        projectsListenerKey = realtimeListeners.attachProjectsListener(uid) { projects ->
            val inProgressProjects = projects.filter { it.status == "in_progress" }
            
            // Cache all projects to SQLite for offline access
            syncManager.cacheProjects(projects)
            
            // Update UI
            projectsAdapter.setProjects(inProgressProjects)
            tvUpcomingCount.text = inProgressProjects.size.toString()
        }
    }
    
    private fun loadProjectsFromCache() {
        // Load from SQLite
        val cachedProjects = syncManager.getCachedProjects()
        val inProgressProjects = cachedProjects.filter { it.status == "in_progress" }
        
        if (cachedProjects.isNotEmpty()) {
            projectsAdapter.setProjects(inProgressProjects)
            tvUpcomingCount.text = inProgressProjects.size.toString()
            Toast.makeText(this, "Loaded ${inProgressProjects.size} projects offline", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No offline data available", Toast.LENGTH_SHORT).show()
        }
    }
}