package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class my_tasks : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val dbRef = FirebaseDatabase.getInstance().reference
    private val realtimeListeners = RealtimeListeners()
    
    private lateinit var rvMyTasks: RecyclerView
    private lateinit var tvHeader: TextView
    private lateinit var tasksAdapter: ProjectDetailTasksAdapter
    private val myTasksList = mutableListOf<ProjectTaskItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)  // Reusing layout
        
        auth = FirebaseAuth.getInstance()
        
        // Setup header
        tvHeader = findViewById(R.id.tvHello)
        val userName = UserSession.getName(this)
        tvHeader.text = "My Tasks"
        
        // Setup RecyclerView
        rvMyTasks = findViewById(R.id.rvTasks)
        rvMyTasks.layoutManager = LinearLayoutManager(this)
        tasksAdapter = ProjectDetailTasksAdapter(myTasksList) { task ->
            // Navigate to task detail
            val intent = Intent(this, task_detail::class.java)
            intent.putExtra("taskId", task.id)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        rvMyTasks.adapter = tasksAdapter
        
        // Setup bottom navigation
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.NavItem.PROFILE)
        
        // Load my tasks
        loadMyTasks()
    }
    
    override fun onStart() {
        super.onStart()
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Sign_in::class.java))
            finish()
            return
        }
    }
    
    override fun onStop() {
        super.onStop()
        realtimeListeners.detachAllListeners()
    }
    
    private fun loadMyTasks() {
        val currentUid = auth.currentUser?.uid ?: return
        
        // Load all projects first
        realtimeListeners.attachProjectsListener(currentUid) { projects ->
            myTasksList.clear()
            
            // For each project, load tasks where user is assigned
            projects.forEach { project ->
                val pid = project.id ?: return@forEach
                
                dbRef.child("projectTasks").child(pid).get()
                    .addOnSuccessListener { snapshot ->
                        for (taskSnap in snapshot.children) {
                            val taskId = taskSnap.key ?: continue
                            val title = taskSnap.child("title").getValue(String::class.java) ?: continue
                            val status = taskSnap.child("status").getValue(String::class.java) ?: "in_progress"
                            
                            // Check if current user is assigned
                            val collaborators = taskSnap.child("collaborators").children.mapNotNull { it.key }
                            val assigneeUid = taskSnap.child("assigneeUid").getValue(String::class.java)
                            
                            if (collaborators.contains(currentUid) || assigneeUid == currentUid) {
                                // Skip completed tasks
                                if (status.lowercase() != "done" && status.lowercase() != "completed") {
                                    val hours = taskSnap.child("hours").getValue(Int::class.java) ?: 0
                                    myTasksList.add(ProjectTaskItem(
                                        id = taskId,
                                        title = title,
                                        hours = hours,
                                        status = status
                                    ))
                                }
                            }
                        }
                        
                        tasksAdapter.setTasks(myTasksList)
                        
                        // Update count
                        val tvCount = findViewById<TextView>(R.id.tvUpcomingCount)
                        tvCount.text = "${myTasksList.size} Tasks Assigned to You"
                    }
            }
        }
    }
}
