package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import de.hdodenhof.circleimageview.CircleImageView

class settings_profile : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private lateinit var pushNotificationsSwitch: Switch
    private lateinit var refreshAutoSwitch: Switch
    private lateinit var keepHistoryCheckbox: CheckBox
    private lateinit var controlSiteCheckbox: CheckBox
    private lateinit var storePasswordsCheckbox: CheckBox
    private lateinit var imgProfileSettings: CircleImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_profile)

        // Initialize Views
        pushNotificationsSwitch = findViewById(R.id.push_notifications_data)
        refreshAutoSwitch = findViewById(R.id.refresh_auto)
        keepHistoryCheckbox = findViewById(R.id.checkbox_highdef)
        controlSiteCheckbox = findViewById(R.id.checkbox_standard)
        storePasswordsCheckbox = findViewById(R.id.store_passwords)

        // Back Navigation
        findViewById<ImageView>(R.id.back_arrow).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btn_back).setOnClickListener { finish() }

        // Edit Profile
        findViewById<LinearLayout>(R.id.edit_profile_section).setOnClickListener {
            startActivity(Intent(this, profile_screen::class.java))
            overridePendingTransition(0, 0)
        }

        // Change Password
        findViewById<LinearLayout>(R.id.change_password_section).setOnClickListener {
            startActivity(Intent(this, set_new_password::class.java))
            overridePendingTransition(0, 0)
        }

        // Support
        findViewById<LinearLayout>(R.id.support_section).setOnClickListener {
            Toast.makeText(this, "Support feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // Help
        findViewById<LinearLayout>(R.id.help_section).setOnClickListener {
            Toast.makeText(this, "Help center coming soon", Toast.LENGTH_SHORT).show()
        }

        // FAQ
        findViewById<LinearLayout>(R.id.faq_section).setOnClickListener {
            Toast.makeText(this, "FAQ coming soon", Toast.LENGTH_SHORT).show()
        }

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

        // Load Settings
        loadSettings()
        loadUserProfileImage() // Load image

        // Set Listeners
        pushNotificationsSwitch.setOnCheckedChangeListener { _, isChecked -> saveSetting("pushNotifications", isChecked) }
        refreshAutoSwitch.setOnCheckedChangeListener { _, isChecked -> saveSetting("refreshAuto", isChecked) }
        keepHistoryCheckbox.setOnCheckedChangeListener { _, isChecked -> saveSetting("keepHistory", isChecked) }
        controlSiteCheckbox.setOnCheckedChangeListener { _, isChecked -> saveSetting("controlSiteData", isChecked) }
        storePasswordsCheckbox.setOnCheckedChangeListener { _, isChecked -> saveSetting("storePasswords", isChecked) }
    }

    private fun loadUserProfileImage() {
        val uid = auth.currentUser?.uid ?: return
        database.child("users").child(uid).child("photoBase64").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val photoBase64 = snapshot.getValue(String::class.java)
                if (!photoBase64.isNullOrEmpty()) {
                    try {
                        val decodedString = android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT)
                        val decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        imgProfileSettings.setImageBitmap(decodedByte)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun loadSettings() {
        val uid = auth.currentUser?.uid ?: return
        database.child("users").child(uid).child("settings").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    pushNotificationsSwitch.isChecked = snapshot.child("pushNotifications").getValue(Boolean::class.java) ?: false
                    refreshAutoSwitch.isChecked = snapshot.child("refreshAuto").getValue(Boolean::class.java) ?: false
                    keepHistoryCheckbox.isChecked = snapshot.child("keepHistory").getValue(Boolean::class.java) ?: false
                    controlSiteCheckbox.isChecked = snapshot.child("controlSiteData").getValue(Boolean::class.java) ?: true
                    storePasswordsCheckbox.isChecked = snapshot.child("storePasswords").getValue(Boolean::class.java) ?: false
                }
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
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save setting", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearData() {
        if (!NetworkUtils.isInternetAvailable(this)) {
            startActivity(Intent(this, No_Internet_Connection::class.java))
            overridePendingTransition(0, 0)
            return
        }
        val uid = auth.currentUser?.uid ?: return
        database.child("users").child(uid).child("settings").removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Settings cleared", Toast.LENGTH_SHORT).show()
                // Reset UI defaults
                pushNotificationsSwitch.isChecked = false
                refreshAutoSwitch.isChecked = false
                keepHistoryCheckbox.isChecked = false
                controlSiteCheckbox.isChecked = true
                storePasswordsCheckbox.isChecked = false
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to clear data", Toast.LENGTH_SHORT).show()
            }
    }
}