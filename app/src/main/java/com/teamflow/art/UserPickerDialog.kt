package com.teamflow.art

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class UserPickerDialog(
    private val context: Context,
    private val currentlySelected: List<String>,
    private val onUsersSelected: (List<AssigneeDraft>) -> Unit
) {
    
    private val selectedUsers = mutableListOf<AssigneeDraft>()
    private val allUsers = mutableListOf<UserItem>()
    
    fun show() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_user_picker)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        val rvUsers = dialog.findViewById<RecyclerView>(R.id.rvUsers)
        val btnCancel = dialog.findViewById<TextView>(R.id.btnCancel)
        val btnDone = dialog.findViewById<TextView>(R.id.btnDone)
        
        rvUsers.layoutManager = LinearLayoutManager(context)
        val adapter = UserPickerAdapter(allUsers) { user, isChecked ->
            if (isChecked) {
                selectedUsers.add(AssigneeDraft(uid = user.uid, photoBase64 = user.photoBase64))
            } else {
                selectedUsers.removeAll { it.uid == user.uid }
            }
        }
        rvUsers.adapter = adapter
        
        // Load users from Firebase
        FirebaseDatabase.getInstance().reference.child("users").get()
            .addOnSuccessListener { snapshot ->
                allUsers.clear()
                for (userSnapshot in snapshot.children) {
                    val uid = userSnapshot.key ?: continue
                    val name = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val email = userSnapshot.child("email").getValue(String::class.java) ?: ""
                    val photoBase64 = userSnapshot.child("photoBase64").getValue(String::class.java)
                    
                    val isSelected = currentlySelected.contains(uid)
                    allUsers.add(UserItem(uid, name, email, photoBase64, isSelected))
                    
                    if (isSelected) {
                        selectedUsers.add(AssigneeDraft(uid = uid, photoBase64 = photoBase64))
                    }
                }
                adapter.notifyDataSetChanged()
            }
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnDone.setOnClickListener {
            onUsersSelected(selectedUsers.toList())
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    data class UserItem(
        val uid: String,
        val name: String,
        val email: String,
        val photoBase64: String?,
        var isSelected: Boolean = false
    )
    
    private class UserPickerAdapter(
        private val users: List<UserItem>,
        private val onUserChecked: (UserItem, Boolean) -> Unit
    ) : RecyclerView.Adapter<UserPickerAdapter.ViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_picker, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(users[position])
        }
        
        override fun getItemCount() = users.size
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvName: TextView = view.findViewById(R.id.tvUserName)
            private val tvEmail: TextView = view.findViewById(R.id.tvUserEmail)
            private val checkbox: CheckBox = view.findViewById(R.id.cbUser)
            
            fun bind(user: UserItem) {
                tvName.text = user.name
                tvEmail.text = user.email
                checkbox.isChecked = user.isSelected
                
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    user.isSelected = isChecked
                    onUserChecked(user, isChecked)
                }
                
                itemView.setOnClickListener {
                    checkbox.isChecked = !checkbox.isChecked
                }
            }
        }
    }
}
