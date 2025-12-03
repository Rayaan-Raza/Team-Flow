package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class splash_screen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        supportActionBar?.hide()

        val auth = FirebaseAuth.getInstance()

        // Keep 5 seconds as you requested
        Handler(Looper.getMainLooper()).postDelayed({

            val next = if (auth.currentUser != null) {
                Intent(this, home_page::class.java)
            } else {
                Intent(this, Create_account::class.java)
            }

            next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(next)
            overridePendingTransition(0, 0)
            finish()

        }, 5000)
    }
}
