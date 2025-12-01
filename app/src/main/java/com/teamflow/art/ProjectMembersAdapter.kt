package com.teamflow.art


import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProjectMembersAdapter(
    private val items: MutableList<MemberItem>
) : RecyclerView.Adapter<ProjectMembersAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivAvatar: ImageView = v.findViewById(R.id.ivAvatar)
        val tvInitial: TextView = v.findViewById(R.id.tvInitial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member_avatar, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]

        val base64 = m.photoBase64
        if (!base64.isNullOrBlank()) {
            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.ivAvatar.setImageBitmap(bmp)
                holder.tvInitial.visibility = View.GONE
            } catch (_: Exception) {
                showInitial(holder, m)
            }
        } else {
            showInitial(holder, m)
        }
    }

    private fun showInitial(holder: VH, m: MemberItem) {
        holder.ivAvatar.setImageResource(R.drawable.pfp1) // fallback
        val ch = (m.name.trim().firstOrNull() ?: m.uid.firstOrNull() ?: '?').toString()
        holder.tvInitial.text = ch.uppercase()
        holder.tvInitial.visibility = View.VISIBLE
    }

    fun setMembers(list: List<MemberItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
}
