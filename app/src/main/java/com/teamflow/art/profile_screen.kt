package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

class profile_screen : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var tvUsername: TextView
    private lateinit var imgProfile: CircleImageView

    // Bottom navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navProjects: LinearLayout
    private lateinit var navCalendar: LinearLayout
    private lateinit var navInbox: LinearLayout
    private lateinit var navProfile: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_screen)

        tvUsername = findViewById(R.id.tvUsername)
        imgProfile = findViewById(R.id.imgProfile)

        // Top Bar
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, settings_main::class.java))
            overridePendingTransition(0, 0)
        }

        // Profile Edit (Pencil Icon)
        findViewById<ImageButton>(R.id.btnEditProfilePic).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
            overridePendingTransition(0, 0)
        }

        // Menu Options
        findViewById<LinearLayout>(R.id.btnMyTask).setOnClickListener {
            startActivity(Intent(this, task_list::class.java))
            overridePendingTransition(0, 0)
        }

        findViewById<LinearLayout>(R.id.btnMyProject).setOnClickListener {
             Toast.makeText(this, "My Projects clicked", Toast.LENGTH_SHORT).show()
        }

        // Reports / Analytics
        findViewById<LinearLayout>(R.id.btnReportAnalytics).setOnClickListener {
             Toast.makeText(this, "Reports clicked", Toast.LENGTH_SHORT).show()
        }

        // Edit Profile (Menu Option)
        findViewById<LinearLayout>(R.id.btnEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
            overridePendingTransition(0, 0)
        }

        // Log Out
        findViewById<LinearLayout>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            val intent = Intent(this, Sign_in::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()

        }

        // Bottom Navigation
        bottomNav()

        // Load Data
        loadUserData()

        imgProfile.setOnClickListener {
            getContent.launch("image/*")
            overridePendingTransition(0, 0)
        }
    }

    private val getContent = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        uri?.let { handleImageSelection(it) }
    }

    private fun handleImageSelection(uri: android.net.Uri) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            startActivity(Intent(this, No_Internet_Connection::class.java))
            overridePendingTransition(0, 0)
            return
        }

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            
            // Resize if too big (max 500x500)
            val maxDimension = 500
            val ratio = Math.min(
                maxDimension.toDouble() / originalBitmap.width,
                maxDimension.toDouble() / originalBitmap.height
            )
            val width = (originalBitmap.width * ratio).toInt()
            val height = (originalBitmap.height * ratio).toInt()
            
            val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, width, height, true)

            // Compress to JPEG
            val outputStream = java.io.ByteArrayOutputStream()
            resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64String = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)

            // Save to Firebase
            val uid = auth.currentUser?.uid ?: return
            database.child("users").child(uid).child("photoBase64").setValue(base64String)
                .addOnSuccessListener {
                    imgProfile.setImageBitmap(resizedBitmap)
                    Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        database.child("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java)
                if (name != null) {
                    tvUsername.text = name
                }
                
                val photoBase64 = snapshot.child("photoBase64").getValue(String::class.java)
                if (!photoBase64.isNullOrEmpty()) {
                    try {
                        val decodedString = android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT)
                        val decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        imgProfile.setImageBitmap(decodedByte)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@profile_screen, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun bottomNav(){
        navHome = findViewById(R.id.navHome)
        navProjects = findViewById(R.id.navProjects)
        navCalendar = findViewById(R.id.navCalendar)
        navInbox = findViewById(R.id.navInbox)
        navProfile = findViewById(R.id.navProfile)

        navCalendar.setOnClickListener { Toast.makeText(this, "Calendar", Toast.LENGTH_SHORT).show() }
        navInbox.setOnClickListener { Toast.makeText(this, "Inbox", Toast.LENGTH_SHORT).show() }
        navProfile.setOnClickListener{
            startActivity(Intent(this, profile_screen::class.java))
            overridePendingTransition(0,0)
            finish()
        }

        navHome.setOnClickListener {
            startActivity(Intent(this, home_page::class.java))
            overridePendingTransition(0,0)
            finish()
        }

        navProjects.setOnClickListener {
            //project list
            Toast.makeText(this, "Projects", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, project_list::class.java))
            //overridePendingTransition(0,0)
           // finish()
        }

    }
}