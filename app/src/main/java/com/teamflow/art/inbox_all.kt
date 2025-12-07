package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
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

class inbox_all : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private val dbRef = FirebaseDatabase.getInstance().reference
    
    private lateinit var backArrow: ImageView
    private lateinit var etSearch: EditText
    private lateinit var rvConversations: RecyclerView
    
    private lateinit var conversationsAdapter: ConversationsAdapter
    
    // Users with existing conversations (have exchanged messages)
    private val existingChats = mutableListOf<Conversation>()
    // All users for search
    private val allUsers = mutableListOf<Conversation>()
    // Currently displayed list
    private val displayedUsers = mutableListOf<Conversation>()
    
    private val conversationListeners = mutableMapOf<String, ChildEventListener>()
    
    private var isSearchActive = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox_all)
        
        auth = FirebaseAuth.getInstance()
        
        // Initialize views
        backArrow = findViewById(R.id.backArrow)
        etSearch = findViewById(R.id.etSearch)
        rvConversations = findViewById(R.id.rvConversations)
        
        // Back button
        backArrow.setOnClickListener { finish() }
        
        // Setup RecyclerView
        rvConversations.layoutManager = LinearLayoutManager(this)
        
        conversationsAdapter = ConversationsAdapter(displayedUsers) { conversation ->
            // Open DM chat screen
            val intent = Intent(this, message::class.java)
            intent.putExtra("conversationId", conversation.id)
            intent.putExtra("otherUserId", conversation.otherUserId)
            intent.putExtra("otherUserName", conversation.otherUserName)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        rvConversations.adapter = conversationsAdapter
        
        // Setup search functionality
        setupSearch()
        
        // Setup bottom navigation
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.NavItem.INBOX)
        
        // Load data
        loadAllUsers()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh existing chats when returning from message screen
        loadExistingChats()
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
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove listeners
        conversationListeners.forEach { (path, listener) ->
            dbRef.child(path).removeEventListener(listener)
        }
        conversationListeners.clear()
    }
    
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                isSearchActive = query.isNotEmpty()
                
                if (isSearchActive) {
                    // Search mode: show matching users from all users
                    filterAllUsers(query)
                } else {
                    // Normal mode: show only existing chats
                    showExistingChats()
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun filterAllUsers(query: String) {
        displayedUsers.clear()
        
        val lowerQuery = query.lowercase()
        allUsers.filterTo(displayedUsers) { user ->
            user.otherUserName?.lowercase()?.contains(lowerQuery) == true ||
            user.otherUserEmail?.lowercase()?.contains(lowerQuery) == true
        }
        
        conversationsAdapter.updateConversations(displayedUsers)
    }
    
    private fun showExistingChats() {
        displayedUsers.clear()
        displayedUsers.addAll(existingChats)
        conversationsAdapter.updateConversations(displayedUsers)
    }
    
    private fun loadAllUsers() {
        val currentUid = auth.currentUser?.uid ?: return
        
        // Load all registered users for search
        dbRef.child("users").get()
            .addOnSuccessListener { snapshot ->
                allUsers.clear()
                
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    
                    // Skip current user
                    if (userId == currentUid) continue
                    
                    val userName = userSnapshot.child("name").getValue(String::class.java)
                    val userEmail = userSnapshot.child("email").getValue(String::class.java)
                    val photoBase64 = userSnapshot.child("photoBase64").getValue(String::class.java)
                    
                    val conversation = Conversation(
                        id = "conv_${currentUid}_${userId}",
                        otherUserId = userId,
                        otherUserName = userName,
                        otherUserEmail = userEmail,
                        lastMessage = null,
                        lastMessageTime = 0,
                        hasUnread = false,
                        photoBase64 = photoBase64
                    )
                    
                    allUsers.add(conversation)
                }
                
                // Sort alphabetically
                allUsers.sortBy { it.otherUserName }
                
                // Load existing chats
                loadExistingChats()
            }
            .addOnFailureListener { e ->
                android.widget.Toast.makeText(this, "Failed to load users: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadExistingChats() {
        val currentUid = auth.currentUser?.uid ?: return
        
        existingChats.clear()
        
        // Check each user for existing conversations
        for (user in allUsers) {
            val otherUserId = user.otherUserId ?: continue
            
            // Get conversation path
            val convPath = if (currentUid < otherUserId) {
                "conversations/${currentUid}_${otherUserId}"
            } else {
                "conversations/${otherUserId}_${currentUid}"
            }
            
            // Check if conversation exists (has messages)
            dbRef.child(convPath).limitToLast(1).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists() && snapshot.childrenCount > 0) {
                        // Get last message
                        val lastMsgSnapshot = snapshot.children.first()
                        val message = lastMsgSnapshot.getValue(MessageData::class.java)
                        
                        // Create conversation with last message info
                        val chatConversation = user.copy(
                            lastMessage = message?.messageText,
                            lastMessageTime = message?.timestamp ?: 0,
                            hasUnread = message?.senderUid != currentUid && message?.isRead == false
                        )
                        
                        // Add to existing chats if not already present
                        val existingIndex = existingChats.indexOfFirst { it.otherUserId == otherUserId }
                        if (existingIndex >= 0) {
                            existingChats[existingIndex] = chatConversation
                        } else {
                            existingChats.add(chatConversation)
                        }
                        
                        // Sort by most recent
                        existingChats.sortByDescending { it.lastMessageTime }
                        
                        // Update display if not in search mode
                        if (!isSearchActive) {
                            showExistingChats()
                        }
                        
                        // Setup real-time listener for this conversation
                        listenToConversation(currentUid, otherUserId)
                    }
                }
        }
        
        // Initial display
        if (!isSearchActive) {
            showExistingChats()
        }
    }
    
    private fun listenToConversation(currentUid: String, otherUserId: String) {
        val convPath = if (currentUid < otherUserId) {
            "conversations/${currentUid}_${otherUserId}"
        } else {
            "conversations/${otherUserId}_${currentUid}"
        }
        
        // Don't add duplicate listeners
        if (conversationListeners.containsKey(convPath)) return
        
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                updateConversationFromMessage(otherUserId, snapshot)
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                updateConversationFromMessage(otherUserId, snapshot)
            }
            
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        
        dbRef.child(convPath).addChildEventListener(listener)
        conversationListeners[convPath] = listener
    }
    
    private fun updateConversationFromMessage(userId: String, messageSnapshot: DataSnapshot) {
        val currentUid = auth.currentUser?.uid ?: return
        val message = messageSnapshot.getValue(MessageData::class.java) ?: return
        
        // Find or create in existing chats
        val userInfo = allUsers.find { it.otherUserId == userId } ?: return
        
        val chatConversation = userInfo.copy(
            lastMessage = message.messageText,
            lastMessageTime = message.timestamp ?: 0,
            hasUnread = message.senderUid != currentUid && message.isRead == false
        )
        
        val existingIndex = existingChats.indexOfFirst { it.otherUserId == userId }
        if (existingIndex >= 0) {
            existingChats[existingIndex] = chatConversation
        } else {
            existingChats.add(chatConversation)
        }
        
        // Sort by most recent
        existingChats.sortByDescending { it.lastMessageTime }
        
        // Update display if not in search mode
        if (!isSearchActive) {
            showExistingChats()
        }
    }
}