package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class OTP : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnVerify: Button
    private lateinit var tvResend: TextView
    private lateinit var tvEmail: TextView

    private lateinit var etCode1: EditText
    private lateinit var etCode2: EditText
    private lateinit var etCode3: EditText
    private lateinit var etCode4: EditText
    private lateinit var etCode5: EditText

    private lateinit var auth: FirebaseAuth
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        auth = FirebaseAuth.getInstance()

        btnBack = findViewById(R.id.btnBack)
        btnVerify = findViewById(R.id.btnVerify)
        tvResend = findViewById(R.id.tvResend)
        tvEmail = findViewById(R.id.tvEmail)

        etCode1 = findViewById(R.id.etCode1)
        etCode2 = findViewById(R.id.etCode2)
        etCode3 = findViewById(R.id.etCode3)
        etCode4 = findViewById(R.id.etCode4)
        etCode5 = findViewById(R.id.etCode5)

        // Get email from previous screen
        email = intent.getStringExtra("email")
        tvEmail.text = email ?: "your email"

        btnBack.setOnClickListener {
            finish()
        }

        btnVerify.setOnClickListener {
            verifyCodeAndContinue()
        }

        tvResend.setOnClickListener {
            resendResetEmail()
        }
    }

    private fun verifyCodeAndContinue() {
        // This is mostly UI / UX, not real Firebase OTP
        val code = listOf(
            etCode1.text.toString(),
            etCode2.text.toString(),
            etCode3.text.toString(),
            etCode4.text.toString(),
            etCode5.text.toString()
        ).joinToString("")

        if (code.length != 5) {
            Toast.makeText(this, "Please enter the 5-digit code", Toast.LENGTH_SHORT).show()
            return
        }

        // We can't actually verify this code via Firebase email-reset
        // So we treat this as a confirmation step AFTER user used email link.
        Toast.makeText(
            this,
            "If you have reset your password using the email link, you can now sign in.",
            Toast.LENGTH_LONG
        ).show()

        // Go back to login
        startActivity(Intent(this, Sign_in::class.java))
        finish()
    }

    private fun resendResetEmail() {
        val mail = email

        if (mail.isNullOrEmpty() || !Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            Toast.makeText(this, "Invalid email to resend reset link", Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(mail)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Reset email resent to $mail",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val msg = task.exception?.localizedMessage
                        ?: "Failed to resend email. Try again."
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
            }
    }
}
