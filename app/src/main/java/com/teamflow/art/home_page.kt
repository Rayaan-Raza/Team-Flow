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

    // Bottom navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navProjects: LinearLayout
    private lateinit var navCalendar: LinearLayout
    private lateinit var navInbox: LinearLayout
    private lateinit var navProfile: LinearLayout

    private lateinit var projectsAdapter: ProjectsAdapter
    private val projectList = mutableListOf<Project>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        auth = FirebaseAuth.getInstance()

        tvHello = findViewById(R.id.tvHello)
        tvUpcomingCount = findViewById(R.id.tvUpcomingCount)
        btnNewTask = findViewById(R.id.btnNewTask)
        rvTasks = findViewById(R.id.rvTasks)

        navHome = findViewById(R.id.navHome)
        navProjects = findViewById(R.id.navProjects)
        navCalendar = findViewById(R.id.navCalendar)
        navInbox = findViewById(R.id.navInbox)
        navProfile = findViewById(R.id.navProfile)

        rvTasks.layoutManager = LinearLayoutManager(this)
        projectsAdapter = ProjectsAdapter(projectList) { project ->
            val pid = project.id ?: return@ProjectsAdapter
            val i = Intent(this, project_detail::class.java)
            i.putExtra("projectId", pid)
            startActivity(i)
        }

        rvTasks.adapter = projectsAdapter

        loadUserName()
        loadAssignedProjects()

        btnNewTask.setOnClickListener {
            // For now: create project
            startActivity(Intent(this, add_edit_project::class.java))
        }

        navHome.setOnClickListener { }
        navProjects.setOnClickListener {
            Toast.makeText(this, "Projects", Toast.LENGTH_SHORT).show()
        }
        navCalendar.setOnClickListener { Toast.makeText(this, "Calendar", Toast.LENGTH_SHORT).show() }
        navInbox.setOnClickListener { Toast.makeText(this, "Inbox", Toast.LENGTH_SHORT).show() }
        navProfile.setOnClickListener { Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show() }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Sign_in::class.java))
            finish()
        }
    }

    private fun loadUserName() {
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

    private fun loadAssignedProjects() {
        val uid = auth.currentUser?.uid ?: return

        dbRef.child("userProjects").child(uid).get()
            .addOnSuccessListener { assignedSnap ->
                val ids = assignedSnap.children.mapNotNull { it.key }
                if (ids.isEmpty()) {
                    projectsAdapter.setProjects(emptyList())
                    tvUpcomingCount.text = "0"
                    return@addOnSuccessListener
                }

                val results = mutableListOf<Project>()
                var remaining = ids.size

                for (pid in ids) {
                    dbRef.child("projects").child(pid).get()
                        .addOnSuccessListener { pSnap ->
                            val p = pSnap.getValue(Project::class.java)
                            if (p != null && (p.status ?: "in_progress") == "in_progress") {
                                results.add(p.copy(id = p.id ?: pid))
                            }
                        }
                        .addOnCompleteListener {
                            remaining--
                            if (remaining == 0) {
                                // Sort newest first (optional)
                                results.sortByDescending { it.createdAt ?: 0L }
                                projectsAdapter.setProjects(results)
                                tvUpcomingCount.text = results.size.toString()
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load projects", Toast.LENGTH_SHORT).show()
            }
    }
}
