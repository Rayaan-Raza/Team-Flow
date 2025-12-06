package com.teamflow.art

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

/**
 * FCM Token Manager
 * Manages FCM token lifecycle and registration
 */
object FcmTokenManager {
    
    private const val TAG = "FcmTokenManager"
    
    /**
     * Get current FCM token and send to server
     */
    fun registerToken(context: Context, uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                if (token != null) {
                    sendTokenToServer(context, uid, token)
                    saveTokenToFirebase(uid, token)
                }
            } else {
                Log.e(TAG, "Failed to get FCM token", task.exception)
            }
        }
    }
    
    /**
     * Send FCM token to backend server (MySQL)
     */
    fun sendTokenToServer(context: Context, uid: String, token: String) {
        val request = object : StringRequest(
            Request.Method.POST,
            ApiConfig.REGISTER_FCM_TOKEN,
            { response ->
                Log.d(TAG, "FCM token registered successfully: $response")
            },
            { error ->
                Log.e(TAG, "Failed to register FCM token", error)
            }
        ) {
            override fun getParams(): Map<String, String> {
                return hashMapOf(
                    "user_uid" to uid,
                    "token" to token,
                    "device_id" to android.provider.Settings.Secure.getString(
                        context.contentResolver,
                        android.provider.Settings.Secure.ANDROID_ID
                    )
                )
            }
        }
        
        VolleyHelper.getInstance(context).addToRequestQueue(request)
    }
    
    /**
     * Save FCM token to Firebase Realtime Database
     */
    private fun saveTokenToFirebase(uid: String, token: String) {
        val dbRef = FirebaseDatabase.getInstance().reference
        dbRef.child("users").child(uid).child("fcmToken").setValue(token)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token saved to Firebase")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save FCM token to Firebase", e)
            }
    }
}
