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
import com.google.firebase.database.*

class home_page : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

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

    private lateinit var tasksAdapter: TasksAdapter
    private val taskList = mutableListOf<Task>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().reference

        tvHello = findViewById(R.id.tvHello)
        tvUpcomingCount = findViewById(R.id.tvUpcomingCount)
        btnNewTask = findViewById(R.id.btnNewTask)
        rvTasks = findViewById(R.id.rvTasks)

        // Bottom navigation bindings
        navHome = findViewById(R.id.navHome)
        navProjects = findViewById(R.id.navProjects)
        navCalendar = findViewById(R.id.navCalendar)
        navInbox = findViewById(R.id.navInbox)
        navProfile = findViewById(R.id.navProfile)

        // RecyclerView Setup
        rvTasks.layoutManager = LinearLayoutManager(this)
        tasksAdapter = TasksAdapter(taskList)
        rvTasks.adapter = tasksAdapter

        // Load data
        loadUserName()
        loadUpcomingTasks()

        // New task button
        btnNewTask.setOnClickListener {
            Toast.makeText(this, "New Task clicked", Toast.LENGTH_SHORT).show()
        }

        // --- BOTTOM NAV CLICKS ---
        navHome.setOnClickListener {
            // Already here
            Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
        }

        navProjects.setOnClickListener {
            Toast.makeText(this, "Projects", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, ProjectsActivity::class.java))
        }

        navCalendar.setOnClickListener {
            Toast.makeText(this, "Calendar", Toast.LENGTH_SHORT).show()
        }

        navInbox.setOnClickListener {
            Toast.makeText(this, "Inbox", Toast.LENGTH_SHORT).show()
        }

        navProfile.setOnClickListener {
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
        }
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
            .addOnSuccessListener { snapshot ->
                val name = snapshot.child("name").getValue(String::class.java)
                tvHello.text = if (!name.isNullOrEmpty()) "Hi, $name" else "Hi,"
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user info", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUpcomingTasks() {
        val uid = auth.currentUser?.uid ?: return

        dbRef.child("tasks").get()
            .addOnSuccessListener { snapshot ->
                val allTasks = mutableListOf<Task>()

                for (projectSnap in snapshot.children) {
                    val projectId = projectSnap.key ?: continue

                    for (taskSnap in projectSnap.children) {
                        val task = taskSnap.getValue(Task::class.java)
                        if (task != null) {
                            val fixedTask = task.copy(
                                id = task.id ?: taskSnap.key,
                                projectId = task.projectId ?: projectId
                            )
                            allTasks.add(fixedTask)
                        }
                    }
                }

                val upcoming = allTasks.filter { t ->
                    (t.assigneeUid == uid || t.assigneeUid.isNullOrEmpty()) &&
                            t.isCompleted != true
                }

                tasksAdapter.updateTasks(upcoming)
                tvUpcomingCount.text = upcoming.size.toString()

                if (upcoming.isEmpty()) {
                    Toast.makeText(this, "No upcoming tasks.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load tasks", Toast.LENGTH_SHORT).show()
            }
    }
}
