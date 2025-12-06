package com.teamflow.art

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class analytics_screen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val dbRef = FirebaseDatabase.getInstance().reference
    private val realtimeListeners = RealtimeListeners()
    
    private lateinit var tvTotalProjects: TextView
    private lateinit var tvCompletedProjects: TextView
    private lateinit var tvInProgressProjects: TextView
    private lateinit var tvTotalTasks: TextView
    private lateinit var tvCompletedTasks: TextView
    private lateinit var tvCompletionRate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)  // Reusing layout
        
        auth = FirebaseAuth.getInstance()
        
        // Setup header
        val tvHeader = findViewById<TextView>(R.id.tvHello)
        tvHeader.text = "Analytics & Reports"
        
        // Setup stats views
        tvTotalProjects = findViewById(R.id.tvUpcomingCount)
        
        // Setup bottom navigation
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.NavItem.PROFILE)
        
        // Load analytics
        loadAnalytics()
    }
    
    private fun loadAnalytics() {
        val currentUid = auth.currentUser?.uid ?: return
        
        realtimeListeners.attachProjectsListener(currentUid) { projects ->
            var totalProjects = 0
            var completedProjects = 0
            var inProgressProjects = 0
            var totalTasks = 0
            var completedTasks = 0
            
            projects.forEach { project ->
                totalProjects++
                
                when (project.status?.lowercase()) {
                    "completed" -> completedProjects++
                    "in_progress" -> inProgressProjects++
                }
                
                totalTasks += project.tasksTotal ?: 0
                completedTasks += project.tasksDone ?: 0
            }
            
            val completionRate = if (totalTasks > 0) {
                (completedTasks.toFloat() / totalTasks.toFloat() * 100).toInt()
            } else 0
            
            // Display stats
            val statsText = """
                📊 Project Statistics
                
                Total Projects: $totalProjects
                Completed: $completedProjects
                In Progress: $inProgressProjects
                
                📋 Task Statistics
                
                Total Tasks: $totalTasks
                Completed: $completedTasks
                Pending: ${totalTasks - completedTasks}
                
                ✅ Completion Rate: $completionRate%
            """.trimIndent()
            
            tvTotalProjects.text = statsText
        }
    }
}
