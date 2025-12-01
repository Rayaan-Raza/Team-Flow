package com.teamflow.art

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProjectsAdapter(
    private val projects: MutableList<Project>,
    private val onClick: (Project) -> Unit
) : RecyclerView.Adapter<ProjectsAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: LinearLayout = itemView.findViewById(R.id.taskCardRoot)
        val tvTag: TextView = itemView.findViewById(R.id.tvTag)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDesc: TextView = itemView.findViewById(R.id.tvDescription)
        val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_home_task, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = projects.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = projects[position]
        holder.tvTag.text = "Project"
        holder.tvTitle.text = p.name ?: "Untitled project"
        holder.tvDesc.text = p.description ?: ""

        val done = p.tasksDone ?: 0
        val total = p.tasksTotal ?: 1
        holder.tvProgress.text = "$done/$total"

        holder.root.setOnClickListener { onClick(p) }
    }

    fun setProjects(list: List<Project>) {
        projects.clear()
        projects.addAll(list)
        notifyDataSetChanged()
    }
}
