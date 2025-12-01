package com.teamflow.art

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ProjectTasksDraftAdapter(
    private val tasks: MutableList<TaskDraft>
) : RecyclerView.Adapter<ProjectTasksDraftAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivStatus: ImageView = itemView.findViewById(R.id.ivStatus)
        val etTitle: EditText = itemView.findViewById(R.id.etTaskTitle)
        val etHours: EditText = itemView.findViewById(R.id.etHours)

        var titleWatcher: TextWatcher? = null
        var hoursWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project_task_row, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = tasks.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = tasks[position]

        // Prevent duplicate watchers
        holder.titleWatcher?.let { holder.etTitle.removeTextChangedListener(it) }
        holder.hoursWatcher?.let { holder.etHours.removeTextChangedListener(it) }

        holder.etTitle.setText(item.title)
        holder.etHours.setText(item.hours?.toString() ?: "")

        holder.titleWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    tasks[pos].title = s?.toString()?.trim().orEmpty()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        holder.hoursWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val v = s?.toString()?.trim()
                    tasks[pos].hours = v?.toIntOrNull()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        holder.etTitle.addTextChangedListener(holder.titleWatcher)
        holder.etHours.addTextChangedListener(holder.hoursWatcher)
    }

    fun addEmptyTask() {
        tasks.add(TaskDraft())
        notifyItemInserted(tasks.size - 1)
    }

    fun getTasks(): List<TaskDraft> = tasks
}
