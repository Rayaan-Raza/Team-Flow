package com.teamflow.art

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

data class MemberAvatar(val uid: String, val photoBase64: String?)

class MembersAdapter(private val items: MutableList<MemberAvatar>) :
    RecyclerView.Adapter<MembersAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val iv: ImageView = v.findViewById(R.id.ivAvatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_member_avatar, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        val b64 = m.photoBase64

        if (!b64.isNullOrBlank()) {
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.iv.setImageBitmap(bmp)
            } catch (_: Exception) {
                holder.iv.setImageResource(R.drawable.pfp1)
            }
        } else {
            holder.iv.setImageResource(R.drawable.pfp1)
        }
    }

    fun setData(list: List<MemberAvatar>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
}
