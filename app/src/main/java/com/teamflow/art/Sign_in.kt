package com.teamflow.art

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class Sign_in : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbRemember: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var tvSignup: TextView
    private lateinit var tvForgot: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        cbRemember = findViewById(R.id.cbRemember)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignup = findViewById(R.id.tvSignup)
        tvForgot = findViewById(R.id.tvForgot)

        // Load remembered credentials (only if remember == true)
        loadRememberedUser()

        btnLogin.setOnClickListener { loginUser() }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, Create_account::class.java))
            overridePendingTransition(0, 0)
        }

        tvForgot.setOnClickListener {
            startActivity(Intent(this, forgot_password::class.java))
            overridePendingTransition(0, 0)
        }
    }

    override fun onStart() {
        super.onStart()
        // If already logged in, go home
        if (auth.currentUser != null) {
            goToHome()
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

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

        if (password.isEmpty()) {
            etPassword.error = "Password required"
            etPassword.requestFocus()
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            etPassword.error = "At least 6 characters"
            etPassword.requestFocus()
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "Signing in..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                btnLogin.isEnabled = true
                btnLogin.text = "Log In"

                if (task.isSuccessful) {
                    if (cbRemember.isChecked) saveUser(email, password) else clearUser()
                    
                    // Save user data to session
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                    loadUserDataAndSave(uid)
                    
                    // Register FCM token
                    FcmTokenManager.registerToken(this, uid)
                    
                    // Mark account exists on device
                    UserSession.markAccountExists(this)

                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    goToHome()
                } else {
                    val e = task.exception
                    val message = when (e) {
                        is FirebaseAuthInvalidUserException -> "No account found with this email"
                        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password"
                        else -> e?.localizedMessage ?: "Login failed. Please try again."
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun goToHome() {
        val intent = Intent(this, home_page::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun saveUser(email: String, password: String) {
        val prefs = getSharedPreferences("teamflow_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("email", email)
            .putString("password", password)
            .putBoolean("remember", true)
            .apply()
    }

    private fun loadRememberedUser() {
        val prefs = getSharedPreferences("teamflow_prefs", Context.MODE_PRIVATE)
        val remember = prefs.getBoolean("remember", false)
        cbRemember.isChecked = remember

        if (remember) {
            etEmail.setText(prefs.getString("email", ""))
            etPassword.setText(prefs.getString("password", ""))
        }
    }

    private fun clearUser() {
        val prefs = getSharedPreferences("teamflow_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
    
    private fun loadUserDataAndSave(uid: String) {
        val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().reference
        dbRef.child("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.child("name").getValue(String::class.java) ?: ""
                val email = snapshot.child("email").getValue(String::class.java) ?: ""
                val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)
                UserSession.saveUser(this, uid, name, email, photoUrl)
            }
    }
}
