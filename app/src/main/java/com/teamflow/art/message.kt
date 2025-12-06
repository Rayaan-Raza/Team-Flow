package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

class message : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private val dbRef = FirebaseDatabase.getInstance().reference
    
    private lateinit var tvOtherUserName: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnSend: ImageView
    private lateinit var etMessage: EditText
    private lateinit var rvMessages: RecyclerView
    
    private lateinit var messagesAdapter: MessagesAdapter
    private val messages = mutableListOf<MessageData>()
    
    private var conversationId: String? = null
    private var otherUserId: String? = null
    private var otherUserName: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        
        auth = FirebaseAuth.getInstance()
        
        // Get intent data
        conversationId = intent.getStringExtra("conversationId")
        otherUserId = intent.getStringExtra("otherUserId")
        otherUserName = intent.getStringExtra("otherUserName")
        
        // Initialize views
        tvOtherUserName = findViewById(R.id.tvOtherUserName)
        btnBack = findViewById(R.id.btnBack)
        btnSend = findViewById(R.id.btnSend)
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
        
        // Send button
        btnSend.setOnClickListener {
            sendMessage()
        }
        
        // Load messages
        loadMessages()
    }
    
    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return
        
        val currentUid = auth.currentUser?.uid ?: return
        val otherUid = otherUserId ?: return
        
        val messageId = dbRef.child("messages").push().key ?: return
        
        val message = MessageData(
            id = messageId,
            messageId = messageId,
            senderUid = currentUid,
            messageText = text,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        
        // Save to Firebase
        dbRef.child("messages").child(messageId).setValue(message)
            .addOnSuccessListener {
                etMessage.setText("")
            }
        
        // Also save to conversation-specific path for easier querying
        val convPath = if (currentUid < otherUid) {
            "conversations/${currentUid}_${otherUid}"
        } else {
            "conversations/${otherUid}_${currentUid}"
        }
        
        dbRef.child(convPath).child(messageId).setValue(message)
    }
    
    private fun loadMessages() {
        val currentUid = auth.currentUser?.uid ?: return
        val otherUid = otherUserId ?: return
        
        val convPath = if (currentUid < otherUid) {
            "conversations/${currentUid}_${otherUid}"
        } else {
            "conversations/${otherUid}_${currentUid}"
        }
        
        dbRef.child(convPath).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(MessageData::class.java)
                if (message != null) {
                    messages.add(message)
                    messagesAdapter.notifyItemInserted(messages.size - 1)
                    rvMessages.scrollToPosition(messages.size - 1)
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}