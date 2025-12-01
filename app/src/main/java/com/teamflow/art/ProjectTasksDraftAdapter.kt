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
        val root: View = itemView
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

    fun setTasks(list: List<TaskDraft>) {
        tasks.clear()
        tasks.addAll(list)
        if (tasks.isEmpty()) tasks.add(TaskDraft())
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = tasks[position]

        // remove old watchers (RecyclerView reuses views)
        holder.titleWatcher?.let { holder.etTitle.removeTextChangedListener(it) }
        holder.hoursWatcher?.let { holder.etHours.removeTextChangedListener(it) }

        holder.etTitle.setText(item.title)

        // hours: show empty string if 0 or null
        val hoursValue = item.hours ?: 0
        holder.etHours.setText(if (hoursValue == 0) "" else hoursValue.toString())

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
                    tasks[pos].hours = v?.toIntOrNull() ?: 0   // SAFE (no crash)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        holder.etTitle.addTextChangedListener(holder.titleWatcher)
        holder.etHours.addTextChangedListener(holder.hoursWatcher)

        // LONG PRESS DELETE
        holder.root.setOnLongClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                tasks.removeAt(pos)
                notifyItemRemoved(pos)

                // keep at least one row
                if (tasks.isEmpty()) {
                    tasks.add(TaskDraft())
                    notifyItemInserted(0)
                }
            }
            true
        }
    }

    fun addEmptyTask() {
        tasks.add(TaskDraft()) // id=null => treated as NEW in updateProject()
        notifyItemInserted(tasks.size - 1)
    }

    fun getTasks(): List<TaskDraft> = tasks
}
