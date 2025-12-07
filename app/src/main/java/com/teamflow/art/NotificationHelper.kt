package com.teamflow.art

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Helper class to send push notifications via Vercel serverless functions
 */
object NotificationHelper {
    
    // TODO: Replace with your Vercel deployment URL after deploying
    private const val VERCEL_URL = "https://your-project.vercel.app"
    
    /**
     * Send notification to a user by their Firebase UID
     * Used for: new messages, task completion, etc.
     */
    suspend fun notifyUser(
        uid: String,
        title: String,
        body: String,
        type: String,
        data: Map<String, String> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$VERCEL_URL/api/notify-user")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val jsonBody = JSONObject().apply {
                put("uid", uid)
                put("title", title)
                put("body", body)
                put("type", type)
                put("data", JSONObject(data))
            }
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode == 200
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error sending notification: ${e.message}")
            false
        }
    }
    
    /**
     * Send message notification to receiver
     */
    suspend fun notifyNewMessage(
        receiverUid: String,
        senderName: String,
        messagePreview: String
    ): Boolean {
        return notifyUser(
            uid = receiverUid,
            title = "New message from $senderName",
            body = if (messagePreview.length > 50) "${messagePreview.take(50)}..." else messagePreview,
            type = "message",
            data = mapOf("senderName" to senderName)
        )
    }
    
    /**
     * Send task completed notification
     */
    suspend fun notifyTaskCompleted(
        ownerUid: String,
        completedByName: String,
        taskTitle: String,
        taskId: String
    ): Boolean {
        return notifyUser(
            uid = ownerUid,
            title = "Task Completed ✓",
            body = "$completedByName completed: $taskTitle",
            type = "task_done",
            data = mapOf("taskId" to taskId, "completedBy" to completedByName)
        )
    }
}
