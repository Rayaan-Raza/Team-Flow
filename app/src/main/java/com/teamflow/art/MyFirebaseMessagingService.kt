package com.teamflow.art

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging Service
 * Handles incoming push notifications and token management
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID_MESSAGES = "messages_channel"
        private const val CHANNEL_ID_TASKS = "tasks_channel"
        private const val CHANNEL_ID_PROJECTS = "projects_channel"
        private const val CHANNEL_ID_GENERAL = "general_channel"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Send token to backend server
        val uid = UserSession.getUid(this)
        if (uid != null) {
            FcmTokenManager.sendTokenToServer(this, uid, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        // Create notification channels
        createNotificationChannels()
        
        // Get notification data
        val title = message.notification?.title ?: message.data["title"] ?: "Team-Flow"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val type = message.data["type"] ?: "general"
        val projectId = message.data["projectId"]
        val taskId = message.data["taskId"]
        
        // Show notification
        showNotification(title, body, type, projectId, taskId)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Messages channel
            val messagesChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New message notifications"
                enableVibration(true)
            }
            
            // Tasks channel
            val tasksChannel = NotificationChannel(
                CHANNEL_ID_TASKS,
                "Tasks",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Task assignment and update notifications"
            }
            
            // Projects channel
            val projectsChannel = NotificationChannel(
                CHANNEL_ID_PROJECTS,
                "Projects",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Project invite and update notifications"
            }
            
            // General channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "General app notifications"
            }
            
            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(tasksChannel)
            notificationManager.createNotificationChannel(projectsChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    private fun showNotification(title: String, body: String, type: String, projectId: String?, taskId: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Determine channel and intent based on type
        val channelId = when (type) {
            "message" -> CHANNEL_ID_MESSAGES
            "task_assigned", "task_completed", "task_updated" -> CHANNEL_ID_TASKS
            "project_invite", "project_updated" -> CHANNEL_ID_PROJECTS
            else -> CHANNEL_ID_GENERAL
        }
        
        // Create intent to open relevant screen
        val intent = when (type) {
            "message" -> Intent(this, message::class.java).apply {
                putExtra("conversationId", projectId ?: taskId)
            }
            "task_assigned", "task_updated" -> Intent(this, task_detail::class.java).apply {
                putExtra("taskId", taskId)
            }
            "project_invite", "project_updated" -> Intent(this, project_detail::class.java).apply {
                putExtra("projectId", projectId)
            }
            else -> Intent(this, home_page::class.java)
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo_main)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
