package com.teamflow.art

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter(
    private val items: MutableList<NotificationData> = mutableListOf(),
    private val onItemClick: (NotificationData) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSub: TextView = view.findViewById(R.id.tvSub)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_task_item, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val notification = items[position]
        
        holder.tvTitle.text = notification.title ?: "Notification"
        holder.tvSub.text = notification.body ?: ""
        
        // Format timestamp
        val time = notification.timestamp
        if (time != null) {
            val now = System.currentTimeMillis()
            val diff = now - time
            
            holder.tvTime.text = when {
                diff < 60 * 1000 -> "Just now"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
                else -> {
                    val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
                    fmt.format(Date(time))
                }
            }
        } else {
            holder.tvTime.text = ""
        }
        
        // Set icon based on notification type
        when (notification.type) {
            "task_completed" -> holder.ivAvatar.setImageResource(R.drawable.task_done)
            "subtask_completed" -> holder.ivAvatar.setImageResource(R.drawable.task_done)
            else -> holder.ivAvatar.setImageResource(R.drawable.tag)
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(notification)
        }
    }

    fun setNotifications(list: List<NotificationData>) {
        items.clear()
        items.addAll(list.sortedByDescending { it.timestamp })
        notifyDataSetChanged()
    }
    
    fun addNotification(notification: NotificationData) {
        items.add(0, notification)
        notifyItemInserted(0)
    }
}
