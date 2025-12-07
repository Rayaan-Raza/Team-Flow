package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class notifications_fill : AppCompatActivity() {
    
    private val dbRef = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    
    private lateinit var rvNotifications: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var btnBack: ImageView
    
    private lateinit var adapter: NotificationsAdapter
    private val notificationsList = mutableListOf<NotificationData>()
    
    private var notificationsListener: ValueEventListener? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications_fill)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        bindViews()
        setupRecyclerView()
        
        btnBack.setOnClickListener { finish() }
    }
    
    override fun onStart() {
        super.onStart()
        startNotificationsListener()
    }
    
    override fun onStop() {
        super.onStop()
        stopNotificationsListener()
    }
    
    private fun bindViews() {
        rvNotifications = findViewById(R.id.rvNotifications)
        emptyState = findViewById(R.id.emptyState)
        btnBack = findViewById(R.id.btnBack)
    }
    
    private fun setupRecyclerView() {
        adapter = NotificationsAdapter(notificationsList) { notification ->
            // Navigate to relevant screen based on notification type
            when (notification.type) {
                "task_completed" -> {
                    val pid = notification.projectId
                    val tid = notification.taskId
                    if (pid != null && tid != null) {
                        val intent = Intent(this, task_detail::class.java)
                        intent.putExtra("projectId", pid)
                        intent.putExtra("taskId", tid)
                        startActivity(intent)
                    }
                }
                "subtask_completed" -> {
                    val pid = notification.projectId
                    val tid = notification.taskId
                    val sid = notification.subTaskId
                    if (pid != null && tid != null && sid != null) {
                        val intent = Intent(this, add_subtask::class.java)
                        intent.putExtra("projectId", pid)
                        intent.putExtra("taskId", tid)
                        intent.putExtra("subTaskId", sid)
                        startActivity(intent)
                    }
                }
                else -> {
                    // For other types, navigate to project detail if projectId exists
                    val pid = notification.projectId
                    if (pid != null) {
                        val intent = Intent(this, project_detail::class.java)
                        intent.putExtra("projectId", pid)
                        startActivity(intent)
                    }
                }
            }
            
            // Mark as read
            val uid = auth.currentUser?.uid ?: return@NotificationsAdapter
            val notifId = notification.id ?: return@NotificationsAdapter
            dbRef.child("notifications").child(uid).child(notifId).child("isRead").setValue(true)
        }
        
        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = adapter
    }
    
    private fun startNotificationsListener() {
        val uid = auth.currentUser?.uid ?: return
        
        notificationsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<NotificationData>()
                
                for (notifSnap in snapshot.children) {
                    val notification = notifSnap.getValue(NotificationData::class.java)
                    if (notification != null) {
                        list.add(notification.copy(id = notifSnap.key))
                    }
                }
                
                adapter.setNotifications(list)
                
                // Show/hide empty state
                if (list.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    rvNotifications.visibility = View.GONE
                } else {
                    emptyState.visibility = View.GONE
                    rvNotifications.visibility = View.VISIBLE
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("NOTIFICATIONS", "Failed to load notifications: ${error.message}")
            }
        }
        
        dbRef.child("notifications").child(uid)
            .orderByChild("timestamp")
            .addValueEventListener(notificationsListener!!)
    }
    
    private fun stopNotificationsListener() {
        val uid = auth.currentUser?.uid ?: return
        notificationsListener?.let {
            dbRef.child("notifications").child(uid).removeEventListener(it)
        }
        notificationsListener = null
    }
}