package com.teamflow.art

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class message : AppCompatActivity() {
    
    companion object {
        // CHANGE THIS TO YOUR XAMPP SERVER IP
        private const val API_BASE_URL = "http://192.168.18.94/TeamFlow/backend/"  // For emulator
        // Use your actual IP for physical device: "http://192.168.18.94/TeamFlow/backend/"
    }
    
    private lateinit var auth: FirebaseAuth
    
    private lateinit var tvOtherUserName: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnSend: ImageView
    private lateinit var btnAttach: ImageView
    private lateinit var etMessage: EditText
    private lateinit var rvMessages: RecyclerView
    
    private lateinit var messagesAdapter: MessagesAdapter
    private val messages = mutableListOf<MessageData>()
    
    private var conversationId: String? = null
    private var otherUserId: String? = null
    private var otherUserName: String? = null
    // hehe
    private var selectedImageBase64: String? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleImageSelection(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        
        auth = FirebaseAuth.getInstance()
        
        // Get intent data
        conversationId = intent.getStringExtra("conversationId")
        otherUserId = intent.getStringExtra("otherUserId")
        otherUserName = intent.getStringExtra("otherUserName")
        
        // Generate conversation ID if not provided
        if (conversationId.isNullOrEmpty() && otherUserId != null) {
            val currentUid = auth.currentUser?.uid ?: ""
            // Use conv_ prefix to match inbox_all.kt format
            conversationId = if (currentUid < otherUserId!!) {
                "conv_${currentUid}_${otherUserId}"
            } else {
                "conv_${otherUserId}_${currentUid}"
            }
        }
        
        // Initialize views
        tvOtherUserName = findViewById(R.id.tvOtherUserName)
        btnBack = findViewById(R.id.btnBack)
        btnSend = findViewById(R.id.btnSend)
        btnAttach = findViewById(R.id.btnAttach)
        etMessage = findViewById(R.id.etMessage)
        rvMessages = findViewById(R.id.rvMessages)
        
        // Set other user name
        tvOtherUserName.text = otherUserName ?: "Chat"
        
        // Setup RecyclerView
        rvMessages.layoutManager = LinearLayoutManager(this)
        messagesAdapter = MessagesAdapter(messages, auth.currentUser?.uid ?: "")
        rvMessages.adapter = messagesAdapter
        
        // Back button
        btnBack.setOnClickListener { finish() }
        
        // Attach button - open gallery
        btnAttach.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        
        // Send button
        btnSend.setOnClickListener {
            sendMessage()
        }
        
        // Load messages via REST API
        loadMessagesFromApi()
    }
    
    private fun handleImageSelection(uri: android.net.Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            
            // Resize if too big (max 800x800 for messaging)
            val maxDimension = 800
            val ratio = minOf(
                maxDimension.toDouble() / originalBitmap.width,
                maxDimension.toDouble() / originalBitmap.height
            )
            val width = (originalBitmap.width * ratio).toInt()
            val height = (originalBitmap.height * ratio).toInt()
            
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
            
            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            selectedImageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
            
            // Show feedback
            Toast.makeText(this, "Image selected - tap send to share", Toast.LENGTH_SHORT).show()
            etMessage.hint = "📷 Image ready to send"
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        val imageBase64 = selectedImageBase64
        
        if (text.isEmpty() && imageBase64 == null) {
            Toast.makeText(this, "Enter a message or attach an image", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "Network required to send message", Toast.LENGTH_SHORT).show()
            return
        }
        
        val currentUid = auth.currentUser?.uid ?: return
        val convId = conversationId ?: return
        
        val messageId = "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val timestamp = System.currentTimeMillis()
        
        // Clear input immediately
        etMessage.setText("")
        etMessage.hint = "Send message"
        selectedImageBase64 = null
        
        // Add message to UI immediately
        val newMessage = MessageData(
            messageId = messageId,
            conversationId = convId,
            senderUid = currentUid,
            messageText = if (text.isNotEmpty()) text else null,
            imageBase64 = imageBase64,
            timestamp = timestamp,
            isRead = false
        )
        messages.add(newMessage)
        messagesAdapter.notifyItemInserted(messages.size - 1)
        rvMessages.scrollToPosition(messages.size - 1)
        
        // Send via REST API
        scope.launch {
            try {
                val success = sendMessageToApi(messageId, convId, currentUid, text, imageBase64, timestamp)
                if (!success) {
                    Toast.makeText(this@message, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@message, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun sendMessageToApi(
        messageId: String,
        conversationId: String,
        senderUid: String,
        messageText: String,
        imageBase64: String?,
        timestamp: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${API_BASE_URL}send_message.php")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            
            // Build POST data
            val postData = StringBuilder()
            postData.append("message_id=").append(URLEncoder.encode(messageId, "UTF-8"))
            postData.append("&conversation_id=").append(URLEncoder.encode(conversationId, "UTF-8"))
            postData.append("&sender_uid=").append(URLEncoder.encode(senderUid, "UTF-8"))
            postData.append("&message_text=").append(URLEncoder.encode(messageText, "UTF-8"))
            postData.append("&timestamp=").append(timestamp)
            
            if (!imageBase64.isNullOrEmpty()) {
                postData.append("&image_base64=").append(URLEncoder.encode(imageBase64, "UTF-8"))
            }
            
            connection.outputStream.use { os ->
                os.write(postData.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode == 200
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun loadMessagesFromApi() {
        val convId = conversationId ?: run {
            android.util.Log.e("MESSAGE", "No conversation ID")
            Toast.makeText(this, "No conversation ID", Toast.LENGTH_SHORT).show()
            return
        }
        
        android.util.Log.d("MESSAGE", "Loading messages for: $convId")
        
        scope.launch {
            try {
                val fetchedMessages = getMessagesFromApi(convId)
                android.util.Log.d("MESSAGE", "Fetched ${fetchedMessages.size} messages")
                messages.clear()
                messages.addAll(fetchedMessages)
                messagesAdapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) {
                    rvMessages.scrollToPosition(messages.size - 1)
                } else {
                    android.util.Log.d("MESSAGE", "No messages found for conversation")
                }
            } catch (e: Exception) {
                android.util.Log.e("MESSAGE", "Error loading: ${e.message}", e)
                Toast.makeText(this@message, "Error loading messages: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun getMessagesFromApi(conversationId: String): List<MessageData> = withContext(Dispatchers.IO) {
        try {
            val urlStr = "${API_BASE_URL}get_messages.php?conversation_id=${URLEncoder.encode(conversationId, "UTF-8")}"
            android.util.Log.d("MESSAGE", "API URL: $urlStr")
            
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            android.util.Log.d("MESSAGE", "Response code: $responseCode")
            
            if (responseCode != 200) {
                android.util.Log.e("MESSAGE", "HTTP error: $responseCode")
                return@withContext emptyList()
            }
            
            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()
            
            android.util.Log.d("MESSAGE", "Response: ${response.take(500)}")
            
            val json = JSONObject(response)
            if (json.getBoolean("success")) {
                val data = json.getJSONObject("data")
                val messagesArray = data.getJSONArray("messages")
                
                val result = mutableListOf<MessageData>()
                for (i in 0 until messagesArray.length()) {
                    val msg = messagesArray.getJSONObject(i)
                    result.add(MessageData(
                        messageId = msg.optString("message_id"),
                        conversationId = msg.optString("conversation_id"),
                        senderUid = msg.optString("sender_uid"),
                        receiverUid = if (msg.isNull("receiver_uid")) null else msg.optString("receiver_uid"),
                        senderName = msg.optString("sender_name"),
                        senderEmail = msg.optString("sender_email"),
                        messageText = if (msg.isNull("message_text")) null else msg.optString("message_text"),
                        imageBase64 = if (msg.isNull("image_base64")) null else msg.optString("image_base64"),
                        timestamp = msg.optLong("timestamp"),
                        isRead = msg.optBoolean("is_read")
                    ))
                }
                result
            } else {
                android.util.Log.e("MESSAGE", "API returned success=false: ${json.optString("error")}")
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("MESSAGE", "Exception: ${e.message}", e)
            emptyList()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}