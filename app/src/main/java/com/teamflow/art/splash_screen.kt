package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class splash_screen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Optional: hide the action bar for a cleaner splash
        supportActionBar?.hide()

        // Delay for 2 seconds then go to LoginActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, Sign_in::class.java)
            startActivity(intent)
            finish()
        }, 5000)
    }
}
