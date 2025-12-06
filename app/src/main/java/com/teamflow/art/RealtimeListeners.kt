package com.teamflow.art

import com.google.firebase.database.*

/**
 * Real-time Listeners Manager
 * Centralized management of Firebase Realtime Database listeners
 */
class RealtimeListeners {
    
    private val listeners = mutableMapOf<String, ValueEventListener>()
    private val childListeners = mutableMapOf<String, ChildEventListener>()
    
    /**
     * Attach a value event listener for projects
     */
    fun attachProjectsListener(uid: String, callback: (List<Project>) -> Unit): String {
        val key = "projects_$uid"
        val dbRef = FirebaseDatabase.getInstance().reference
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val projects = mutableListOf<Project>()
                
                // Get user's project IDs
                dbRef.child("userProjects").child(uid).get().addOnSuccessListener { userProjectsSnap ->
                    val projectIds = userProjectsSnap.children.mapNotNull { it.key }
                    
                    if (projectIds.isEmpty()) {
                        callback(emptyList())
                        return@addOnSuccessListener
                    }
                    
                    var remaining = projectIds.size
                    for (pid in projectIds) {
                        dbRef.child("projects").child(pid).get().addOnSuccessListener { pSnap ->
                            val project = pSnap.getValue(Project::class.java)
                            if (project != null) {
                                projects.add(project.copy(id = pid))
                            }
                            remaining--
                            if (remaining == 0) {
                                callback(projects.sortedByDescending { it.createdAt })
                            }
                        }
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        }
        
        dbRef.child("userProjects").child(uid).addValueEventListener(listener)
        listeners[key] = listener
        return key
    }
    
    /**
     * Attach a value event listener for tasks in a project
     */
    fun attachTasksListener(projectId: String, callback: (List<ProjectTask>) -> Unit): String {
        val key = "tasks_$projectId"
        val dbRef = FirebaseDatabase.getInstance().reference
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<ProjectTask>()
                for (child in snapshot.children) {
                    val task = child.getValue(ProjectTask::class.java)
                    if (task != null) {
                        tasks.add(task)
                    }
                }
                callback(tasks)
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        }
        
        dbRef.child("projectTasks").child(projectId).addValueEventListener(listener)
        listeners[key] = listener
        return key
    }
    
    /**
     * Attach a child event listener for messages (real-time delivery)
     */
    fun attachMessagesListener(conversationId: String, callback: (MessageData, String) -> Unit): String {
        val key = "messages_$conversationId"
        val dbRef = FirebaseDatabase.getInstance().reference
        
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(MessageData::class.java)
                if (message != null) {
                    callback(message, "added")
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(MessageData::class.java)
                if (message != null) {
                    callback(message, "changed")
                }
            }
            
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val message = snapshot.getValue(MessageData::class.java)
                if (message != null) {
                    callback(message, "removed")
                }
            }
            
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        
        dbRef.child("messages").child(conversationId).addChildEventListener(listener)
        childListeners[key] = listener
        return key
    }
    
    /**
     * Attach a value event listener for notifications
     */
    fun attachNotificationsListener(uid: String, callback: (List<NotificationData>) -> Unit): String {
        val key = "notifications_$uid"
        val dbRef = FirebaseDatabase.getInstance().reference
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = mutableListOf<NotificationData>()
                for (child in snapshot.children) {
                    val notification = child.getValue(NotificationData::class.java)
                    if (notification != null) {
                        notifications.add(notification.copy(id = child.key))
                    }
                }
                callback(notifications.sortedByDescending { it.timestamp })
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        }
        
        dbRef.child("notifications").child(uid).addValueEventListener(listener)
        listeners[key] = listener
        return key
    }
    
    /**
     * Detach a specific listener
     */
    fun detachListener(key: String) {
        listeners[key]?.let { listener ->
            // Determine the reference path from the key
            val dbRef = FirebaseDatabase.getInstance().reference
            when {
                key.startsWith("projects_") -> {
                    val uid = key.removePrefix("projects_")
                    dbRef.child("userProjects").child(uid).removeEventListener(listener)
                }
                key.startsWith("tasks_") -> {
                    val projectId = key.removePrefix("tasks_")
                    dbRef.child("projectTasks").child(projectId).removeEventListener(listener)
                }
                key.startsWith("notifications_") -> {
                    val uid = key.removePrefix("notifications_")
                    dbRef.child("notifications").child(uid).removeEventListener(listener)
                }
            }
            listeners.remove(key)
        }
        
        childListeners[key]?.let { listener ->
            val dbRef = FirebaseDatabase.getInstance().reference
            when {
                key.startsWith("messages_") -> {
                    val conversationId = key.removePrefix("messages_")
                    dbRef.child("messages").child(conversationId).removeEventListener(listener)
                }
            }
            childListeners.remove(key)
        }
    }
    
    /**
     * Detach all listeners
     */
    fun detachAllListeners() {
        listeners.keys.toList().forEach { detachListener(it) }
        childListeners.keys.toList().forEach { detachListener(it) }
    }
}
