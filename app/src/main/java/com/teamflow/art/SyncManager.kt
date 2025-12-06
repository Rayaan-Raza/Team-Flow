package com.teamflow.art

import android.content.Context
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import kotlinx.coroutines.*

/**
 * Sync Manager
 * Handles synchronization between SQLite cache and Firebase
 */
class SyncManager(private val context: Context) {
    
    private val dbHelper = DatabaseHelper(context)
    private val dbRef = FirebaseDatabase.getInstance().reference
    private val gson = Gson()
    private val TAG = "SyncManager"
    
    /**
     * Sync all pending operations to Firebase
     */
    suspend fun syncPendingOperations() = withContext(Dispatchers.IO) {
        val pendingOps = dbHelper.getPendingOperations()
        
        for (op in pendingOps) {
            try {
                when (op.entityType) {
                    "project" -> syncProject(op)
                    "task" -> syncTask(op)
                    "subtask" -> syncSubTask(op)
                    "message" -> syncMessage(op)
                }
                
                // Delete operation after successful sync
                dbHelper.deletePendingOperation(op.id)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync operation ${op.id}", e)
            }
        }
    }
    
    private suspend fun syncProject(op: PendingOperation) = suspendCancellableCoroutine<Unit> { continuation ->
        val project = gson.fromJson(op.operationData, Project::class.java)
        val projectId = op.entityId
        
        when (op.operationType) {
            "create", "update" -> {
                dbRef.child("projects").child(projectId).setValue(project)
                    .addOnSuccessListener { continuation.resume(Unit) {} }
                    .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
            }
            "delete" -> {
                dbRef.child("projects").child(projectId).removeValue()
                    .addOnSuccessListener { continuation.resume(Unit) {} }
                    .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
            }
        }
    }
    
    private suspend fun syncTask(op: PendingOperation) = suspendCancellableCoroutine<Unit> { continuation ->
        val task = gson.fromJson(op.operationData, Task::class.java)
        val taskId = op.entityId
        val projectId = task.projectId ?: return@suspendCancellableCoroutine
        
        when (op.operationType) {
            "create", "update" -> {
                dbRef.child("projectTasks").child(projectId).child(taskId).setValue(task)
                    .addOnSuccessListener { continuation.resume(Unit) {} }
                    .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
            }
            "delete" -> {
                dbRef.child("projectTasks").child(projectId).child(taskId).removeValue()
                    .addOnSuccessListener { continuation.resume(Unit) {} }
                    .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
            }
        }
    }
    
    private suspend fun syncSubTask(op: PendingOperation) = suspendCancellableCoroutine<Unit> { continuation ->
        val subtask = gson.fromJson(op.operationData, SubTask::class.java)
        val subtaskId = op.entityId
        val taskId = subtask.taskId ?: return@suspendCancellableCoroutine
        
        when (op.operationType) {
            "create", "update" -> {
                dbRef.child("subTasks").child(taskId).child(subtaskId).setValue(subtask)
                    .addOnSuccessListener { continuation.resume(Unit) {} }
                    .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
            }
            "delete" -> {
                dbRef.child("subTasks").child(taskId).child(subtaskId).removeValue()
                    .addOnSuccessListener { continuation.resume(Unit) {} }
                    .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
            }
        }
    }
    
    private suspend fun syncMessage(op: PendingOperation) = suspendCancellableCoroutine<Unit> { continuation ->
        val message = gson.fromJson(op.operationData, MessageData::class.java)
        val messageId = op.entityId
        val conversationId = message.conversationId ?: return@suspendCancellableCoroutine
        
        when (op.operationType) {
            "create", "update" -> {
                dbRef.child("messages").child(conversationId).child(messageId).setValue(message)
                    .addOnSuccessListener { continuation.resume(Unit) {} }
                    .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
            }
            "delete" -> {
                dbRef.child("messages").child(conversationId).child(messageId).removeValue()
                    .addOnSuccessListener { continuation.resume(Unit) {} }
                    .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
            }
        }
    }
    
    /**
     * Cache projects from Firebase to SQLite
     */
    fun cacheProjects(projects: List<Project>) {
        for (project in projects) {
            val projectId = project.id ?: continue
            val json = gson.toJson(project)
            dbHelper.insertOrUpdate(DatabaseHelper.TABLE_PROJECTS, projectId, json, true)
        }
        dbHelper.updateSyncTime("projects")
    }
    
    /**
     * Cache tasks from Firebase to SQLite
     */
    fun cacheTasks(tasks: List<Task>) {
        for (task in tasks) {
            val taskId = task.id ?: continue
            val json = gson.toJson(task)
            dbHelper.insertOrUpdate(DatabaseHelper.TABLE_TASKS, taskId, json, true)
        }
        dbHelper.updateSyncTime("tasks")
    }
    
    /**
     * Get cached projects from SQLite
     */
    fun getCachedProjects(): List<Project> {
        val jsonList = dbHelper.getAll(DatabaseHelper.TABLE_PROJECTS)
        return jsonList.mapNotNull { json ->
            try {
                gson.fromJson(json, Project::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Get cached tasks from SQLite
     */
    fun getCachedTasks(): List<Task> {
        val jsonList = dbHelper.getAll(DatabaseHelper.TABLE_TASKS)
        return jsonList.mapNotNull { json ->
            try {
                gson.fromJson(json, Task::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
