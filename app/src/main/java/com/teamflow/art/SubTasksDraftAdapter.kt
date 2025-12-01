package com.teamflow.art

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class SubTasksDraftAdapter(
    private val items: MutableList<SubTaskDraft>
) : RecyclerView.Adapter<SubTasksDraftAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val root: View = v
        val ivStatus: ImageView = v.findViewById(R.id.ivStatus)
        val etTitle: EditText = v.findViewById(R.id.etTaskTitle)
        val etHours: EditText = v.findViewById(R.id.etHours)

        var titleWatcher: TextWatcher? = null
        var hoursWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project_task_row, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]

        holder.titleWatcher?.let { holder.etTitle.removeTextChangedListener(it) }
        holder.hoursWatcher?.let { holder.etHours.removeTextChangedListener(it) }

        holder.etTitle.setText(it.title)
        holder.etHours.setText(if (it.hours == 0) "" else it.hours.toString())

        holder.titleWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) items[pos].title = s?.toString()?.trim().orEmpty()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        holder.hoursWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    items[pos].hours = s?.toString()?.trim()?.toIntOrNull() ?: 0
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        holder.etTitle.addTextChangedListener(holder.titleWatcher)
        holder.etHours.addTextChangedListener(holder.hoursWatcher)

        // long press delete
        holder.root.setOnLongClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                items.removeAt(pos)
                notifyItemRemoved(pos)
                if (items.isEmpty()) {
                    items.add(SubTaskDraft())
                    notifyItemInserted(0)
                }
            }
            true
        }
    }

    fun addEmpty() {
        items.add(SubTaskDraft())
        notifyItemInserted(items.size - 1)
    }

    fun setItems(list: List<SubTaskDraft>) {
        items.clear()
        items.addAll(list)
        if (items.isEmpty()) items.add(SubTaskDraft())
        notifyDataSetChanged()
    }

    fun getItems(): MutableList<SubTaskDraft> = items
}
