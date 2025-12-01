import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.teamflow.art.R

data class MemberMini(val uid: String, val photoBase64: String?)

class MembersMiniAdapter(
    private val items: MutableList<MemberMini> = mutableListOf()
) : RecyclerView.Adapter<MembersMiniAdapter.VH>() {

    fun submit(newItems: List<MemberMini>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val iv: ImageView = v.findViewById(R.id.ivAvatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_member_avatar, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size.coerceAtMost(10) // show max 10 (optional)

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        val b64 = m.photoBase64

        if (b64.isNullOrBlank()) {
            holder.iv.setImageDrawable(null)
            holder.iv.setBackgroundResource(R.drawable.bg_avatar_fallback_red) // fallback red circle
            return
        }

        try {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            holder.iv.setBackgroundResource(0)
            holder.iv.setImageBitmap(bmp)
        } catch (_: Exception) {
            holder.iv.setImageDrawable(null)
            holder.iv.setBackgroundResource(R.drawable.bg_avatar_fallback_red)
        }
    }
}
