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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        auth = FirebaseAuth.getInstance()

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
        startProjectsListener()
    }

    override fun onPause() {
        super.onPause()
        projectsListenerKey?.let { realtimeListeners.detachListener(it) }
    }


    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        startProjectsListener()
        if (currentUser == null) {
            startActivity(Intent(this, Sign_in::class.java))
            overridePendingTransition(0, 0)
            finish()
            return
        }
        
        // Load user name from UserSession
        val userName = UserSession.getName(this)
        tvHello.text = if (!userName.isNullOrEmpty()) "Hi, $userName" else "Hi,"
        
        // Load user name from Firebase if not in session
        if (userName.isNullOrEmpty()) {
            loadUserName()
        }
    }

    private fun loadUserName() {
        if (!NetworkUtils.isInternetAvailable(this)) {
            startActivity(Intent(this, No_Internet_Connection::class.java))
            overridePendingTransition(0, 0)
            return
        }
        val uid = auth.currentUser?.uid ?: return
        dbRef.child("users").child(uid).get()
            .addOnSuccessListener { snap ->
                val name = snap.child("name").getValue(String::class.java)
                tvHello.text = if (!name.isNullOrEmpty()) "Hi, $name" else "Hi,"
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user info", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startProjectsListener() {
        if (!NetworkUtils.isInternetAvailable(this)) {
            startActivity(Intent(this, No_Internet_Connection::class.java))
            overridePendingTransition(0, 0)
            return
        }
        val uid = auth.currentUser?.uid ?: return

        android.util.Log.d("HOME_PAGE", "Attaching projects listener for uid: $uid")
        Toast.makeText(this, "DEBUG: Listening for projects of user $uid", Toast.LENGTH_SHORT).show()
        
        projectsListenerKey = realtimeListeners.attachProjectsListener(uid) { projects ->
            android.util.Log.d("HOME_PAGE", "Received ${projects.size} projects from listener")
            Toast.makeText(this, "DEBUG: Received ${projects.size} total projects", Toast.LENGTH_SHORT).show()
            
            val inProgressProjects = projects.filter { it.status == "in_progress" }
            android.util.Log.d("HOME_PAGE", "After filter: ${inProgressProjects.size} in_progress projects")
            Toast.makeText(this, "DEBUG: ${inProgressProjects.size} in_progress projects", Toast.LENGTH_SHORT).show()
            
            // Log each project for debugging
            projects.forEach { p ->
                android.util.Log.d("HOME_PAGE", "Project: id=${p.id}, name=${p.name}, status=${p.status}")
            }
            
            projectsAdapter.setProjects(inProgressProjects)
            tvUpcomingCount.text = inProgressProjects.size.toString()
        }
    }


}