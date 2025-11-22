package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Create_account : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirm: EditText
    private lateinit var cbTerms: CheckBox
    private lateinit var btnBack: ImageView
    private lateinit var btnSignUp: Button
    private lateinit var tvSignIn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        auth = FirebaseAuth.getInstance()

        // Initialize views
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirm = findViewById(R.id.etConfirm)
        cbTerms = findViewById(R.id.cbTerms)
        btnBack = findViewById(R.id.btnBack)
        btnSignUp = findViewById(R.id.btnSignUp)
        tvSignIn = findViewById(R.id.tvSignIn)

        btnBack.setOnClickListener { finish() }
        tvSignIn.setOnClickListener {
            startActivity(Intent(this, Sign_in::class.java))
            finish()
        }

        btnSignUp.setOnClickListener { registerUser() }
    }

    private fun registerUser() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirm.text.toString().trim()

        // === VALIDATION ===

        if (name.isEmpty()) {
            etName.error = "Name required"
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
            etName.requestFocus()
            return
        }

        if (email.isEmpty()) {
            etEmail.error = "Email required"
            Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show()
            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Invalid email format"
            Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password required"
            Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show()
            etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            etPassword.error = "At least 6 characters"
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            etPassword.requestFocus()
            return
        }

        if (confirmPassword != password) {
            etConfirm.error = "Passwords do not match"
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            etConfirm.requestFocus()
            return
        }

        if (!cbTerms.isChecked) {
            Toast.makeText(this, "Please accept the Terms & Policy", Toast.LENGTH_SHORT).show()
            return
        }

        // === CREATE ACCOUNT ===

        btnSignUp.isEnabled = false
        btnSignUp.text = "Creating..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                btnSignUp.isEnabled = true
                btnSignUp.text = "SIGN UP"

                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                    // Build user node
                    val user = mapOf(
                        "id" to uid,
                        "name" to name,
                        "email" to email,
                        "photoUrl" to null,
                        "createdAt" to System.currentTimeMillis()
                    )

                    // Write user into Realtime DB
                    FirebaseDatabase.getInstance().reference
                        .child("users")
                        .child(uid)
                        .setValue(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()

                            // Navigate to SignIn or Home directly:
                            startActivity(Intent(this, home_page::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to save user data", Toast.LENGTH_LONG).show()
                        }

                } else {
                    val msg = task.exception?.localizedMessage ?: "Sign up failed"
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
            }
    }
}
