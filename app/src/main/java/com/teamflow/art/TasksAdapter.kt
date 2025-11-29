package com.teamflow.art

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TasksAdapter(
    private val tasks: MutableList<Task>
) : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardRoot: LinearLayout = itemView.findViewById(R.id.taskCardRoot)
        val tvTag: TextView = itemView.findViewById(R.id.tvTag)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
        val tvAttachments: TextView = itemView.findViewById(R.id.tvAttachments)
        val tvComments: TextView = itemView.findViewById(R.id.tvComments)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun getItemCount(): Int = tasks.size

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        // Priority as tag (or category if you store it)
        holder.tvTag.text = (task.priority ?: "Task").replaceFirstChar { it.uppercase() }

        holder.tvTitle.text = task.title ?: "Untitled task"
        holder.tvDescription.text = task.description ?: ""

        // Simple progress: 0/1 or 1/1 based on isCompleted
        val completed = if (task.isCompleted == true) 1 else 0
        holder.tvProgress.text = "$completed/1"

        // Attachments / comments if you later add them to Task, otherwise keep 0
        val attachmentsCount = task.attachmentsCount ?: 0
        val commentsCount = task.commentsCount ?: 0

        holder.tvAttachments.text = " $attachmentsCount Attachment" +
                if (attachmentsCount != 1) "s" else ""
        holder.tvComments.text = " $commentsCount Comment" +
                if (commentsCount != 1) "s" else ""

        // If you want click → open Task Details later:
        // holder.cardRoot.setOnClickListener { ... }
    }

    fun updateTasks(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }
}