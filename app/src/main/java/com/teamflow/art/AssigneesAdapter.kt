package com.teamflow.art

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class AssigneesAdapter(
    private val users: MutableList<AssigneeDraft>
) : RecyclerView.Adapter<AssigneesAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iv: CircleImageView = itemView.findViewById(R.id.ivAvatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_avatar, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = users[position]
        val b64 = u.photoBase64
        if (!b64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.iv.setImageBitmap(bmp)
            } catch (_: Exception) {
                // fallback stays default src
            }
        }
    }

    fun setUsers(newUsers: List<AssigneeDraft>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    fun getUsers(): List<AssigneeDraft> = users
}
