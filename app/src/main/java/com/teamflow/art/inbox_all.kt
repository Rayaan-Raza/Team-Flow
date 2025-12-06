package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private lateinit var tabAll: TextView
    private lateinit var tabUnread: TextView
    private lateinit var tabRead: TextView
    private lateinit var tabUnderlineRow: android.widget.LinearLayout
    private lateinit var rvConversations: RecyclerView
    
    private lateinit var conversationsAdapter: ConversationsAdapter
    private val allUsers = mutableListOf<Conversation>() // All registered users
    private val conversationListeners = mutableMapOf<String, ChildEventListener>()
    
    private enum class TabType { ALL, UNREAD, READ }
    private var currentTab = TabType.ALL
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox_all)
        
        auth = FirebaseAuth.getInstance()
        
        // Initialize views
        backArrow = findViewById(R.id.backArrow)
        tabAll = findViewById(R.id.tabAll)
        tabUnread = findViewById(R.id.tabUnread)
        tabRead = findViewById(R.id.tabRead)
        tabUnderlineRow = findViewById(R.id.tabUnderlineRow)
        rvConversations = findViewById(R.id.rvConversations)
        
        // Back button
        backArrow.setOnClickListener { finish() }
        
        // Setup RecyclerView for conversations
        rvConversations.layoutManager = LinearLayoutManager(this)
        
        conversationsAdapter = ConversationsAdapter(allUsers) { conversation ->
            // Open DM chat screen
            val intent = Intent(this, message::class.java)
            intent.putExtra("conversationId", conversation.id)
            intent.putExtra("otherUserId", conversation.otherUserId)
            intent.putExtra("otherUserName", conversation.otherUserName)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        rvConversations.adapter = conversationsAdapter
        
        // Setup tabs
        setupTabs()
        
        // Setup bottom navigation
        BottomNavHelper.setupBottomNav(this, BottomNavHelper.NavItem.INBOX)
        
        // Load all users
        loadAllUsers()
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
    
    private fun setupTabs() {
        tabAll.setOnClickListener {
            switchTab(TabType.ALL)
        }
        
        tabUnread.setOnClickListener {
            switchTab(TabType.UNREAD)
        }
        
        tabRead.setOnClickListener {
            switchTab(TabType.READ)
        }
        
        // Set initial tab
        switchTab(TabType.ALL)
    }
    
    private fun switchTab(tab: TabType) {
        currentTab = tab
        
        // Reset all tabs to grey
        tabAll.setTextColor(ContextCompat.getColor(this, R.color.textgrey))
        tabUnread.setTextColor(ContextCompat.getColor(this, R.color.textgrey))
        tabRead.setTextColor(ContextCompat.getColor(this, R.color.textgrey))
        tabAll.setTypeface(null, android.graphics.Typeface.NORMAL)
        tabUnread.setTypeface(null, android.graphics.Typeface.NORMAL)
        tabRead.setTypeface(null, android.graphics.Typeface.NORMAL)
        
        // Clear underlines
        for (i in 0 until tabUnderlineRow.childCount) {
            tabUnderlineRow.getChildAt(i).setBackgroundColor(
                ContextCompat.getColor(this, android.R.color.transparent)
            )
        }
        
        // Set selected tab to black with blue underline
        when (tab) {
            TabType.ALL -> {
                tabAll.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                tabAll.setTypeface(null, android.graphics.Typeface.BOLD)
                tabUnderlineRow.getChildAt(0).setBackgroundColor(
                    ContextCompat.getColor(this, R.color.bluebg)
                )
            }
            TabType.UNREAD -> {
                tabUnread.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                tabUnread.setTypeface(null, android.graphics.Typeface.BOLD)
                tabUnderlineRow.getChildAt(1).setBackgroundColor(
                    ContextCompat.getColor(this, R.color.bluebg)
                )
            }
            TabType.READ -> {
                tabRead.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                tabRead.setTypeface(null, android.graphics.Typeface.BOLD)
                tabUnderlineRow.getChildAt(2).setBackgroundColor(
                    ContextCompat.getColor(this, R.color.bluebg)
                )
            }
        }
        
        // Filter conversations based on tab
        filterConversations()
    }
    
    private fun filterConversations() {
        val filtered = when (currentTab) {
            TabType.ALL -> allUsers // Show all registered users
            TabType.UNREAD -> allUsers.filter { it.hasUnread == true }
            TabType.READ -> allUsers.filter { it.hasUnread == false && it.lastMessage != null }
        }
        conversationsAdapter.updateConversations(filtered)
    }
    
    private fun loadAllUsers() {
        val currentUid = auth.currentUser?.uid ?: return
        
        android.util.Log.d("InboxAll", "Loading users for UID: $currentUid")
        
        // Load all registered users
        dbRef.child("users").get()
            .addOnSuccessListener { snapshot ->
                android.util.Log.d("InboxAll", "Firebase snapshot exists: ${snapshot.exists()}, children: ${snapshot.childrenCount}")
                
                allUsers.clear()
                
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    
                    // Skip current user
                    if (userId == currentUid) {
                        android.util.Log.d("InboxAll", "Skipping current user: $userId")
                        continue
                    }
                    
                    val userName = userSnapshot.child("name").getValue(String::class.java)
                    val userEmail = userSnapshot.child("email").getValue(String::class.java)
                    val photoBase64 = userSnapshot.child("photoBase64").getValue(String::class.java)
                    
                    android.util.Log.d("InboxAll", "Adding user: $userName ($userId)")
                    
                    // Create conversation object for each user
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
                    
                    // Listen for messages with this user
                    listenToConversation(currentUid, userId)
                }
                
                android.util.Log.d("InboxAll", "Total users loaded: ${allUsers.size}")
                
                // Sort alphabetically by name
                allUsers.sortBy { it.otherUserName }
                
                // IMPORTANT: Update the adapter to show all users
                filterConversations()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("InboxAll", "Failed to load users", e)
                android.widget.Toast.makeText(this, "Failed to load users: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun listenToConversation(currentUid: String, otherUserId: String) {
        val convPath = if (currentUid < otherUserId) {
            "conversations/${currentUid}_${otherUserId}"
        } else {
            "conversations/${otherUserId}_${currentUid}"
        }
        
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
        
        // Find conversation
        val conversation = allUsers.find { it.otherUserId == userId } ?: return
        val index = allUsers.indexOf(conversation)
        
        // Update conversation with latest message
        allUsers[index] = conversation.copy(
            lastMessage = message.messageText,
            lastMessageTime = message.timestamp ?: 0,
            hasUnread = message.senderUid != currentUid && message.isRead == false
        )
        
        // Re-sort by last message time (most recent first)
        allUsers.sortByDescending { it.lastMessageTime }
        
        filterConversations()
    }
}