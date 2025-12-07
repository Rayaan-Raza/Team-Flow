package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class set_new_password : AppCompatActivity() {

    private lateinit var etPassword: EditText
    private lateinit var etConfirm: EditText
    private lateinit var btnUpdate: Button
    private lateinit var ivShowPass: ImageView
    private lateinit var ivShowConfirm: ImageView
    private val auth = FirebaseAuth.getInstance()

    private var isPassVisible = false
    private var isConfirmVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_new_password)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        etPassword = findViewById(R.id.etPassword)
        etConfirm = findViewById(R.id.etConfirm)
        btnUpdate = findViewById(R.id.btnUpdate)
        ivShowPass = findViewById(R.id.ivShowPass)
        ivShowConfirm = findViewById(R.id.ivShowConfirm)

        ivShowPass.setOnClickListener {
            isPassVisible = !isPassVisible
            togglePassVisibility(etPassword, isPassVisible)
        }

        ivShowConfirm.setOnClickListener {
            isConfirmVisible = !isConfirmVisible
            togglePassVisibility(etConfirm, isConfirmVisible)
        }

        btnUpdate.setOnClickListener {
            updatePassword()
        }
    }

    private fun togglePassVisibility(editText: EditText, isVisible: Boolean) {
        if (isVisible) {
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        editText.setSelection(editText.text.length)
    }

    private fun updatePassword() {
        val password = etPassword.text.toString().trim()
        val confirm = etConfirm.text.toString().trim()

        if (password.length < 6) {
            etPassword.error = "Min 6 chars"
            return
        }
        if (password != confirm) {
            etConfirm.error = "Passwords do not match"
            return
        }

        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "Network required to update password", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        if (user != null && user.email != null) {

            user.updatePassword(password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Sync to Realtime Database
                        val database = FirebaseDatabase.getInstance().reference
                        database.child("users").child(user.uid).child("password").setValue(password)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Auth updated but DB sync failed", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                    } else {
                        Toast.makeText(this, "Failed: " + task.exception?.message, Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
}