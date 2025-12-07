package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class splash_screen : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private val dbRef = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        
        auth = FirebaseAuth.getInstance()
        
        // Delay for splash screen effect
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthenticationStatus()
        }, 2000)
    }
    
    private fun checkAuthenticationStatus() {
        val currentUser = auth.currentUser
        
        when {
            // Case 1: User is logged in
            currentUser != null -> {
                // Check if online - if not, skip Firebase fetch and use cached session
                if (NetworkUtils.isInternetAvailable(this)) {
                    loadUserDataAndNavigateHome(currentUser.uid)
                } else {
                    // Offline: Use cached session data and go to home
                    navigateToHome()
                }
            }
            // Case 2: Account exists on device but not logged in
            UserSession.hasAccount(this) -> {
                navigateToSignIn()
            }
            // Case 3: No account ever created on this device
            else -> {
                navigateToCreateAccount()
            }
        }
    }
    
    private fun loadUserDataAndNavigateHome(uid: String) {
        // Load user data from Firebase
        dbRef.child("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.child("name").getValue(String::class.java) ?: ""
                val email = snapshot.child("email").getValue(String::class.java) ?: ""
                val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)
                
                // Save to UserSession
                UserSession.saveUser(this, uid, name, email, photoUrl)
                
                // Register FCM token
                FcmTokenManager.registerToken(this, uid)
                
                // Send welcome notification
                CoroutineScope(Dispatchers.IO).launch {
                    NotificationHelper.notifyWelcome(uid, name.ifEmpty { "there" })
                }
                
                // Navigate to home
                navigateToHome()
            }
            .addOnFailureListener {
                // If failed to load, still navigate to home
                navigateToHome()
            }
    }
    
    private fun navigateToHome() {
        startActivity(Intent(this, home_page::class.java))
        finish()
    }
    
    private fun navigateToSignIn() {
        startActivity(Intent(this, Sign_in::class.java))
        finish()
    }
    
    private fun navigateToCreateAccount() {
        startActivity(Intent(this, Create_account::class.java))
        finish()
    }
}
