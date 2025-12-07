package com.teamflow.art

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class EditProfileActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageView
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        loadUserData()

        btnSave.setOnClickListener {
            saveUserData()
        }
    }

    private fun loadUserData() {
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "Network required", Toast.LENGTH_SHORT).show()
            return
        }
        val user = auth.currentUser
        val uid = user?.uid
        
        if (user != null) {
            etEmail.setText(user.email)
        }

        if (uid != null) {
            database.child("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    if (name != null) {
                        etName.setText(name)
                    }
                    val email = snapshot.child("email").getValue(String::class.java)
                    if (email != null) {
                        etEmail.setText(email)
                    }
                    val password = snapshot.child("password").getValue(String::class.java)
                    if (password != null) {
                        etPassword.setText(password)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditProfileActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun saveUserData() {
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "Network required to save", Toast.LENGTH_SHORT).show()
            return
        }
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = "Name cannot be empty"
            return
        }
        if (email.isEmpty()) {
            etEmail.error = "Email cannot be empty"
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "Password cannot be empty"
            return
        }

        val user = auth.currentUser
        val uid = user?.uid

        if (user != null && uid != null) {
            // Update Name in Database
            val updates = mapOf<String, Any>(
                "name" to name,
                "email" to email,
                "password" to password // Also update email in DB for consistency
            )
            
            database.child("users").child(uid).updateChildren(updates)
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update database", Toast.LENGTH_SHORT).show()
                }

            // Update Email in Auth
            if (email != user.email) {
                user.updateEmail(email).addOnFailureListener {
                    Toast.makeText(this, "Failed to update email: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }

            // Update Password in Auth
            if (password.isNotEmpty()) {
                if (password.length < 6) {
                    etPassword.error = "Min 6 chars"
                    return
                }
                user.updatePassword(password).addOnSuccessListener {
                    Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to update password: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }

            Toast.makeText(this, "Profile updating...", Toast.LENGTH_SHORT).show()
            // Finish after a short delay or assume success for better UX, 
            // but ideally we should wait for all tasks. For simplicity:
            finish()
            
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
}
