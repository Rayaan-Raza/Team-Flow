package com.teamflow.art

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class settings_main : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_scroll_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<android.view.View>(R.id.back_arrow).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.btn_back).setOnClickListener { finish() }

        // Account Settings
        findViewById<android.view.View>(R.id.profile_info_section).setOnClickListener {
            startActivity(android.content.Intent(this, settings_profile::class.java))
            overridePendingTransition(0, 0)
        }

        // Privacy
        findViewById<android.view.View>(R.id.privacy_section).setOnClickListener {
            startActivity(android.content.Intent(this, settings_privacy::class.java))
            overridePendingTransition(0, 0)
        }

        // Change Password
        findViewById<android.view.View>(R.id.change_password_section).setOnClickListener {
            startActivity(android.content.Intent(this, set_new_password::class.java))
            overridePendingTransition(0, 0)
        }

        // Rate App
        findViewById<android.view.View>(R.id.rate_app_section).setOnClickListener {
            try {
                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$packageName")))
                overridePendingTransition(0, 0)
            } catch (e: android.content.ActivityNotFoundException) {
                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                overridePendingTransition(0, 0)
            }
        }

        // Send Feedback
        findViewById<android.view.View>(R.id.send_feedback_section).setOnClickListener {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:")
                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("support@teamflow.com"))
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Feedback for TeamFlow")
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                overridePendingTransition(0, 0)
            } else {
                 android.widget.Toast.makeText(this, "No email app found", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        // Notifications Settings (Using as entry point for App Settings for now, or add new section)
        // The layout has "Notifications settings" header but the section ID is push_notifications_section
        findViewById<android.view.View>(R.id.push_notifications_section).setOnClickListener {
             startActivity(android.content.Intent(this, settings_app::class.java))
             overridePendingTransition(0, 0)
        }

        // Privacy Policy
        findViewById<android.view.View>(R.id.privacy_policy_section).setOnClickListener {
            val url = "https://www.google.com" // Replace with actual policy URL
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(url)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }
}