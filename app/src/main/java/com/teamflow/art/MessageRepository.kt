package com.teamflow.art

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import org.json.JSONObject

/**
 * Message Repository
 * Handles dual storage for messages (Firebase + MySQL)
 */
class MessageRepository(private val context: Context) {
    
    private val dbRef = FirebaseDatabase.getInstance().reference
    private val gson = Gson()
    private val TAG = "MessageRepository"
    
    /**
     * Send message to both Firebase and MySQL
     */
    fun sendMessage(
        messageId: String,
        conversationId: String,
        senderUid: String,
        messageText: String,
        projectId: String? = null,
        taskId: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val timestamp = System.currentTimeMillis()
        
        // 1. Send to Firebase first (real-time)
        val messageData = mapOf(
            "messageId" to messageId,
            "conversationId" to conversationId,
            "senderUid" to senderUid,
            "messageText" to messageText,
            "timestamp" to timestamp,
            "isRead" to false
        )
        
        dbRef.child("messages").child(conversationId).child(messageId).setValue(messageData)
            .addOnSuccessListener {
                // 2. Then send to MySQL (persistent storage)
                sendToMySQL(messageId, conversationId, senderUid, messageText, timestamp, projectId, taskId, onSuccess, onError)
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Failed to send message to Firebase")
            }
    }
    
    private fun sendToMySQL(
        messageId: String,
        conversationId: String,
        senderUid: String,
        messageText: String,
        timestamp: Long,
        projectId: String?,
        taskId: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val request = object : StringRequest(
            Request.Method.POST,
            ApiConfig.SEND_MESSAGE,
            { response ->
                Log.d(TAG, "Message sent to MySQL: $response")
                onSuccess()
            },
            { error ->
                Log.e(TAG, "Failed to send message to MySQL", error)
                // Don't fail the whole operation if MySQL fails
                onSuccess()  // Message is already in Firebase
            }
        ) {
            override fun getParams(): Map<String, String> {
                return hashMapOf(
                    "message_id" to messageId,
                    "conversation_id" to conversationId,
                    "sender_uid" to senderUid,
                    "message_text" to messageText,
                    "timestamp" to timestamp.toString(),
                    "project_id" to (projectId ?: ""),
                    "task_id" to (taskId ?: "")
                )
            }
        }
        
        VolleyHelper.getInstance(context).addToRequestQueue(request)
    }
    
    /**
     * Get message history from MySQL
     */
    fun getMessageHistory(
        conversationId: String,
        limit: Int = 50,
        offset: Int = 0,
        onSuccess: (List<MessageData>) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "${ApiConfig.GET_MESSAGES}?conversation_id=$conversationId&limit=$limit&offset=$offset"
        
        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val messagesArray = response.getJSONObject("data").getJSONArray("messages")
                        val messages = mutableListOf<MessageData>()
                        
                        for (i in 0 until messagesArray.length()) {
                            val msgObj = messagesArray.getJSONObject(i)
                            messages.add(
                                MessageData(
                                    messageId = msgObj.getString("message_id"),
                                    conversationId = msgObj.getString("conversation_id"),
                                    senderUid = msgObj.getString("sender_uid"),
                                    senderName = msgObj.optString("sender_name"),
                                    senderEmail = msgObj.optString("sender_email"),
                                    messageText = msgObj.getString("message_text"),
                                    timestamp = msgObj.getLong("timestamp"),
                                    isRead = msgObj.getBoolean("is_read")
                                )
                            )
                        }
                        
                        onSuccess(messages)
                    } else {
                        onError(response.optString("error", "Failed to get messages"))
                    }
                } catch (e: Exception) {
                    onError(e.localizedMessage ?: "Failed to parse messages")
                }
            },
            { error ->
                onError(error.localizedMessage ?: "Network error")
            }
        )
        
        VolleyHelper.getInstance(context).addToRequestQueue(request)
    }
    
    /**
     * Mark messages as read
     */
    fun markAsRead(
        conversationId: String,
        userUid: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val request = object : StringRequest(
            Request.Method.POST,
            ApiConfig.MARK_READ,
            { response ->
                Log.d(TAG, "Messages marked as read: $response")
                onSuccess()
            },
            { error ->
                Log.e(TAG, "Failed to mark messages as read", error)
                onError(error.localizedMessage ?: "Failed to mark as read")
            }
        ) {
            override fun getParams(): Map<String, String> {
                return hashMapOf(
                    "conversation_id" to conversationId,
                    "user_uid" to userUid
                )
            }
        }
        
        VolleyHelper.getInstance(context).addToRequestQueue(request)
    }
}
