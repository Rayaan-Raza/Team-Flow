package com.teamflow.art

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class ConversationsAdapter(
    private var conversations: List<Conversation>,
    private val onConversationClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationsAdapter.ViewHolder>() {

    fun updateConversations(newConversations: List<Conversation>) {
        conversations = newConversations
        notifyDataSetChanged()
        android.util.Log.d("ConversationsAdapter", "Updated conversations: ${conversations.size} items")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount() = conversations.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imgProfile: CircleImageView = view.findViewById(R.id.imgProfile)
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage)

        fun bind(conversation: Conversation) {
            tvName.text = conversation.otherUserName ?: "Unknown User"
            tvLastMessage.text = conversation.lastMessage ?: "Start a conversation"
            
            // Apply fade (0.7 alpha) to:
            // 1. Users with no conversation (lastMessage == null)
            // 2. Users with unread messages (hasUnread == true)
            val shouldFade = conversation.lastMessage == null || conversation.hasUnread == true
            val alpha = if (shouldFade) 0.7f else 1.0f
            
            imgProfile.alpha = alpha
            tvName.alpha = alpha
            tvLastMessage.alpha = alpha
            
            // Load profile picture if available
            val photoBase64 = conversation.photoBase64
            if (!photoBase64.isNullOrEmpty()) {
                try {
                    val decodedString = Base64.decode(photoBase64, Base64.DEFAULT)
                    val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    imgProfile.setImageBitmap(decodedByte)
                } catch (e: Exception) {
                    e.printStackTrace()
                    imgProfile.setImageResource(R.drawable.pfp1)
                }
            } else {
                imgProfile.setImageResource(R.drawable.pfp1)
            }
            
            itemView.setOnClickListener {
                onConversationClick(conversation)
            }
        }
    }
}
