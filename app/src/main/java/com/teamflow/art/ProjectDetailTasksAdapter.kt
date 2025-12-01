package com.teamflow.art

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProjectDetailTasksAdapter(
    private val items: MutableList<ProjectTaskItem>
) : RecyclerView.Adapter<ProjectDetailTasksAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivStatus: ImageView = v.findViewById(R.id.ivStatus)
        val tvTitle: TextView = v.findViewById(R.id.tvTaskTitle)
        val tvHours: TextView = v.findViewById(R.id.tvTaskHours)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project_detail_task, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = items[position]
        holder.tvTitle.text = t.title
        holder.tvHours.text = " ${t.hours} hr"

        val s = t.status.lowercase()
        val isDone = (s == "done" || s == "completed")

        holder.ivStatus.setBackgroundResource(
            if (isDone) R.drawable.circle_check else R.drawable.bg_circle_mask
        )
    }

    fun setTasks(list: List<ProjectTaskItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
}
