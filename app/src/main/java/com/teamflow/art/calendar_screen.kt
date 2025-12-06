package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class calendar_screen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val dbRef = FirebaseDatabase.getInstance().reference
    private val realtimeListeners = RealtimeListeners()
    
    private lateinit var rvTasks: RecyclerView
    private lateinit var tasksAdapter: TasksCalendarAdapter
    private val tasksList = mutableListOf<TaskCalendarItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)  // Reusing home page layout
        
        auth = FirebaseAuth.getInstance()
        
        // Load and display user name
        val tvHello = findViewById<TextView>(R.id.tvHello)
        val userName = UserSession.getName(this)
        tvHello?.text = if (!userName.isNullOrEmpty()) "Hi, $userName" else "Hi,"
        
        // Setup RecyclerView for tasks
        rvTasks = findViewById(R.id.rvTasks) ?: return
        rvTasks.layoutManager = LinearLayoutManager(this)
        tasksAdapter = TasksCalendarAdapter(tasksList) { task ->
            val intent = Intent(this, task_detail::class.java)
            intent.putExtra("projectId", task.projectId)
            intent.putExtra("taskId", task.taskId)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        rvTasks.adapter = tasksAdapter
        
        // New task button
        val btnNewTask = findViewById<Button>(R.id.btnNewTask)
        btnNewTask?.setOnClickListener {
            startActivity(Intent(this, add_edit_project::class.java))
            overridePendingTransition(0, 0)
        }
        
        // Setup bottom navigation
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.NavItem.CALENDAR)
        
        // Load tasks by due date
        loadTasksByDueDate()
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
    }
    
    override fun onStop() {
        super.onStop()
        realtimeListeners.detachAllListeners()
    }
    
    private fun loadTasksByDueDate() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        
        // Load all projects user is part of
        realtimeListeners.attachProjectsListener(uid) { projects ->
            tasksList.clear()
            
            val now = System.currentTimeMillis()
            
            // For each project, load its tasks
            projects.forEach { project ->
                val pid = project.id ?: return@forEach
                
                dbRef.child("projectTasks").child(pid).get()
                    .addOnSuccessListener { snapshot ->
                        for (taskSnap in snapshot.children) {
                            val taskId = taskSnap.key ?: continue
                            val title = taskSnap.child("title").getValue(String::class.java) ?: continue
                            val status = taskSnap.child("status").getValue(String::class.java) ?: "in_progress"
                            
                            // Skip completed tasks
                            if (status.lowercase() == "done" || status.lowercase() == "completed") {
                                continue
                            }
                            
                            val dueAt = taskSnap.child("dueAt").getValue(Long::class.java)
                            
                            // Only show tasks with due dates
                            if (dueAt != null && dueAt >= now) {
                                val hours = taskSnap.child("hours").getValue(Int::class.java) ?: 0
                                
                                tasksList.add(TaskCalendarItem(
                                    taskId = taskId,
                                    projectId = pid,
                                    projectName = project.name ?: "Untitled Project",
                                    title = title,
                                    dueAt = dueAt,
                                    hours = hours,
                                    status = status
                                ))
                            }
                        }
                        
                        // Sort by due date (earliest first - most urgent at top)
                        tasksList.sortBy { it.dueAt }
                        tasksAdapter.notifyDataSetChanged()
                        
                        // Update header
                        val tvUpcoming = findViewById<TextView>(R.id.tvUpcomingCount)
                        tvUpcoming.text = "Upcoming Tasks (${tasksList.size})"
                    }
            }
        }
    }
    
    // Data class for calendar tasks
    data class TaskCalendarItem(
        val taskId: String,
        val projectId: String,
        val projectName: String,
        val title: String,
        val dueAt: Long,
        val hours: Int,
        val status: String
    )
    
    // Adapter for calendar tasks
    class TasksCalendarAdapter(
        private val tasks: List<TaskCalendarItem>,
        private val onTaskClick: (TaskCalendarItem) -> Unit
    ) : RecyclerView.Adapter<TasksCalendarAdapter.ViewHolder>() {
        
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_home_task, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(tasks[position])
        }
        
        override fun getItemCount() = tasks.size
        
        inner class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            private val tvTitle: TextView? = view.findViewById(R.id.tvTaskTitle)
            private val tvHours: TextView? = view.findViewById(R.id.tvTaskHours)
            
            fun bind(task: TaskCalendarItem) {
                tvTitle?.text = task.title
                
                // Show due date prominently
                val dueDate = dateFormat.format(Date(task.dueAt))
                val daysUntilDue = ((task.dueAt - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                
                val urgencyText = when {
                    daysUntilDue == 0 -> "⚠️ Due Today"
                    daysUntilDue == 1 -> "⚠️ Due Tomorrow"
                    daysUntilDue <= 3 -> "⚠️ Due in $daysUntilDue days"
                    else -> "Due: $dueDate"
                }
                
                tvHours?.text = "$urgencyText • ${task.projectName}"
                
                itemView.setOnClickListener {
                    onTaskClick(task)
                }
            }
        }
    }
}
