package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class settings_app : AppCompatActivity() {

    private lateinit var switchBackgroundAudio: Switch
    private lateinit var switchCellularData: Switch
    private lateinit var checkboxStandard: CheckBox
    private lateinit var checkboxHighDef: CheckBox
    private lateinit var switchDarkMode: Switch
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val PREFS_NAME = "AppSettings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_app)

        // Initialize Views
        switchBackgroundAudio = findViewById(R.id.switch_background_audio)
        switchCellularData = findViewById(R.id.switch_cellular_data)
        checkboxStandard = findViewById(R.id.checkbox_standard)
        checkboxHighDef = findViewById(R.id.checkbox_highdef)
        switchDarkMode = findViewById(R.id.switch_darkmode)

        // Back Navigation
        findViewById<ImageView>(R.id.back_arrow).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btn_back).setOnClickListener { finish() }

        // Load Settings (Local first for speed, then sync with Firebase)
        loadSettings()

        // Listeners
        switchBackgroundAudio.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("background_audio", isChecked)
        }

        switchCellularData.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("cellular_data", isChecked)
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("dark_mode", isChecked)
            applyDarkMode(isChecked)
        }

        // Mutually Exclusive Checkboxes
        checkboxStandard.setOnClickListener {
            if (checkboxStandard.isChecked) {
                checkboxHighDef.isChecked = false
                saveSetting("quality", "standard")
            } else {
                checkboxStandard.isChecked = true 
            }
        }

        checkboxHighDef.setOnClickListener {
            if (checkboxHighDef.isChecked) {
                checkboxStandard.isChecked = false
                saveSetting("quality", "high_def")
            } else {
                checkboxHighDef.isChecked = true
            }
        }
    }

    private fun applyDarkMode(isDark: Boolean) {
        if (isDark) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun loadSettings() {
        // Load from SharedPreferences first
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        switchBackgroundAudio.isChecked = prefs.getBoolean("background_audio", false)
        switchCellularData.isChecked = prefs.getBoolean("cellular_data", false)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        switchDarkMode.isChecked = isDarkMode
        
        // Ensure theme is applied on load (if not already handled by system)
        // Note: Changing theme here might cause recreation loop if not handled carefully.
        // Usually theme is set in Application class or before setContentView.
        // For this simple implementation, we assume the user toggles it.

        val quality = prefs.getString("quality", "standard")
        if (quality == "high_def") {
            checkboxHighDef.isChecked = true
            checkboxStandard.isChecked = false
        } else {
            checkboxStandard.isChecked = true
            checkboxHighDef.isChecked = false
        }

        // Sync with Firebase
        val uid = auth.currentUser?.uid ?: return
        val settingsRef = database.child("users").child(uid).child("settings").child("app_settings")

        settingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val bgAudio = snapshot.child("background_audio").getValue(Boolean::class.java) ?: false
                    val cellData = snapshot.child("cellular_data").getValue(Boolean::class.java) ?: false
                    val darkMode = snapshot.child("dark_mode").getValue(Boolean::class.java) ?: false
                    val qualityVal = snapshot.child("quality").getValue(String::class.java) ?: "standard"

                    // Update UI and Local Prefs if different
                    if (switchBackgroundAudio.isChecked != bgAudio) {
                        switchBackgroundAudio.isChecked = bgAudio
                        prefs.edit().putBoolean("background_audio", bgAudio).apply()
                    }
                    if (switchCellularData.isChecked != cellData) {
                        switchCellularData.isChecked = cellData
                        prefs.edit().putBoolean("cellular_data", cellData).apply()
                    }
                    if (switchDarkMode.isChecked != darkMode) {
                        switchDarkMode.isChecked = darkMode
                        prefs.edit().putBoolean("dark_mode", darkMode).apply()
                        applyDarkMode(darkMode)
                    }
                    
                    if (qualityVal == "high_def") {
                        if (!checkboxHighDef.isChecked) {
                            checkboxHighDef.isChecked = true
                            checkboxStandard.isChecked = false
                            prefs.edit().putString("quality", "high_def").apply()
                        }
                    } else {
                        if (!checkboxStandard.isChecked) {
                            checkboxStandard.isChecked = true
                            checkboxHighDef.isChecked = false
                            prefs.edit().putString("quality", "standard").apply()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Ignore
            }
        })
    }

    private fun saveSetting(key: String, value: Any) {
        // Save locally
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()
        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is String -> editor.putString(key, value)
        }
        editor.apply()

        // Save to Firebase
        if (!NetworkUtils.isInternetAvailable(this)) {
            startActivity(Intent(this, No_Internet_Connection::class.java))
            overridePendingTransition(0, 0)
            return
        }
        val uid = auth.currentUser?.uid ?: return
        database.child("users").child(uid).child("settings").child("app_settings").child(key).setValue(value)
            .addOnFailureListener {
                Toast.makeText(this, "Failed to sync setting", Toast.LENGTH_SHORT).show()
            }
    }
}