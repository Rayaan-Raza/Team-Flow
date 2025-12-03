package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class settings_options : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_options)

        findViewById<ImageView>(R.id.back_arrow).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<LinearLayout>(R.id.invite_friends_section).setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Check out this cool app!")
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
            overridePendingTransition(0, 0)
        }

        findViewById<LinearLayout>(R.id.account_edit_profile).setOnClickListener {
            val intent = Intent(this, settings_profile::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        findViewById<LinearLayout>(R.id.change_password_section).setOnClickListener {
            // val intent = Intent(this, SettingsChangePasswordActivity::class.java)
            // startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.blocked_users_section).setOnClickListener {
             val intent = Intent(this, settings_privacy::class.java)
             startActivity(intent)
             overridePendingTransition(0, 0)
        }

        findViewById<LinearLayout>(R.id.updates_section).setOnClickListener {
             val intent = Intent(this, settings_app::class.java)
             startActivity(intent)
             overridePendingTransition(0, 0)
        }
    }
}
