package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class settings_privacy : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private lateinit var keepHistoryCheckbox: CheckBox
    private lateinit var controlSiteCheckbox: CheckBox
    private lateinit var storePasswordsCheckbox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_privacy)

        // Back Buttons
        findViewById<ImageView>(R.id.back_arrow).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btn_back).setOnClickListener { finish() }

        // Manage Account -> Edit Profile
        findViewById<LinearLayout>(R.id.manage_account_profile).setOnClickListener {
            startActivity(Intent(this, profile_screen::class.java))
            overridePendingTransition(0, 0)
        }

        // Share My Profile
        findViewById<LinearLayout>(R.id.updates_section).setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "TeamFlow Profile")
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out my profile on TeamFlow!")
            startActivity(Intent.createChooser(shareIntent, "Share via"))
            overridePendingTransition(0, 0)
        }

        // Privacy and Safety
        findViewById<LinearLayout>(R.id.privacy_and_safety_section).setOnClickListener {
            Toast.makeText(this, "Privacy and Safety settings coming soon", Toast.LENGTH_SHORT).show()
        }

        // My Balance
        findViewById<LinearLayout>(R.id.my_balance_section).setOnClickListener {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                database.child("users").child(uid).child("balance").get().addOnSuccessListener {
                    val balance = it.value ?: "0.00"
                    Toast.makeText(this, "Your Balance: $$balance", Toast.LENGTH_LONG).show()
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to fetch balance", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize Checkboxes
        keepHistoryCheckbox = findViewById(R.id.checkbox_highdef)
        controlSiteCheckbox = findViewById(R.id.checkbox_standard)
        storePasswordsCheckbox = findViewById(R.id.store_passwords)

        // Load Settings
        loadSettings()

        // Set Listeners
        keepHistoryCheckbox.setOnCheckedChangeListener { _, isChecked -> saveSetting("keepHistory", isChecked) }
        controlSiteCheckbox.setOnCheckedChangeListener { _, isChecked -> saveSetting("controlSiteData", isChecked) }
        storePasswordsCheckbox.setOnCheckedChangeListener { _, isChecked -> saveSetting("storePasswords", isChecked) }

         // Clear Saved Data
        findViewById<LinearLayout>(R.id.clear_saved_data_section).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear Saved Data")
                .setMessage("Are you sure you want to clear all saved settings? This action cannot be undone.")
                .setPositiveButton("Clear") { _, _ ->
                    clearData()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun clearData() {
        if (!NetworkUtils.isInternetAvailable(this)) {
            startActivity(Intent(this, No_Internet_Connection::class.java))
            overridePendingTransition(0, 0)
            return
        }
        val uid = auth.currentUser?.uid ?: return

        // Clear Firebase Settings
        database.child("users").child(uid).child("settings").removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Settings cleared", Toast.LENGTH_SHORT).show()
                // Reset UI defaults
                keepHistoryCheckbox.isChecked = false
                controlSiteCheckbox.isChecked = true
                storePasswordsCheckbox.isChecked = false
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to clear data", Toast.LENGTH_SHORT).show()
            }

        // Clear Local Preferences
        val prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
        prefs.edit().clear().apply()

    }

    private fun loadSettings() {
        val uid = auth.currentUser?.uid ?: return
        database.child("users").child(uid).child("settings").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                keepHistoryCheckbox.isChecked = snapshot.child("keepHistory").getValue(Boolean::class.java) ?: false
                controlSiteCheckbox.isChecked = snapshot.child("controlSiteData").getValue(Boolean::class.java) ?: true
                storePasswordsCheckbox.isChecked = snapshot.child("storePasswords").getValue(Boolean::class.java) ?: false
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun saveSetting(key: String, value: Boolean) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            startActivity(Intent(this, No_Internet_Connection::class.java))
            overridePendingTransition(0, 0)
            return
        }
        val uid = auth.currentUser?.uid ?: return
        database.child("users").child(uid).child("settings").child(key).setValue(value)
    }
}
