package com.teamflow.art

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter(
    private val messages: MutableList<MessageData>,
    private val currentUserUid: String
) : RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val isSent = message.senderUid == currentUserUid
        holder.bind(message, isSent)
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: MessageData) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun setMessages(newMessages: List<MessageData>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSenderName: TextView = view.findViewById(R.id.tvSenderName)
        private val tvMessageText: TextView = view.findViewById(R.id.tvMessageText)
        private val ivMessageImage: ImageView = view.findViewById(R.id.ivMessageImage)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)
        private val rootLayout: LinearLayout = view as LinearLayout

        fun bind(message: MessageData, isSent: Boolean) {
            // Align message based on sender
            rootLayout.gravity = if (isSent) Gravity.END else Gravity.START
            
            // Show sender name for received messages
            if (!isSent && !message.senderName.isNullOrEmpty()) {
                tvSenderName.visibility = View.VISIBLE
                tvSenderName.text = message.senderName
            } else {
                tvSenderName.visibility = View.GONE
            }
            
            // Handle text
            if (!message.messageText.isNullOrEmpty()) {
                tvMessageText.visibility = View.VISIBLE
                tvMessageText.text = message.messageText
            } else {
                tvMessageText.visibility = View.GONE
            }
            
            // Handle image
            if (!message.imageBase64.isNullOrEmpty()) {
                ivMessageImage.visibility = View.VISIBLE
                try {
                    val decodedBytes = Base64.decode(message.imageBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    ivMessageImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    ivMessageImage.visibility = View.GONE
                }
            } else {
                ivMessageImage.visibility = View.GONE
            }
            
            // Time
            tvTime.text = formatTime(message.timestamp ?: 0)
            tvTime.gravity = if (isSent) Gravity.END else Gravity.START
        }
    }

    private fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000} min ago"
            diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
