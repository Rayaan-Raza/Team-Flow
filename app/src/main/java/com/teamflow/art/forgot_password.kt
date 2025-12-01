package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class forgot_password : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var btnReset: Button
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etEmail)
        btnReset = findViewById(R.id.btnReset)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        btnReset.setOnClickListener { sendResetEmail() }
    }

    private fun sendResetEmail() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Email required"
            etEmail.requestFocus()
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Invalid email format"
            etEmail.requestFocus()
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return
        }

        btnReset.isEnabled = false
        btnReset.text = "Sending..."

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                btnReset.isEnabled = true
                btnReset.text = "Reset Password"

                if (task.isSuccessful) {
                    Toast.makeText(this, "Reset link sent to $email", Toast.LENGTH_LONG).show()

                    val intent = Intent(this, OTP::class.java).apply {
                        putExtra("email", email)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    finish() // <-- prevents going back to forgot_password

                } else {
                    val msg = task.exception?.localizedMessage
                        ?: "Failed to send reset email. Try again."
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
            }
    }
}
