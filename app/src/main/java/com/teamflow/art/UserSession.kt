package com.teamflow.art

import android.content.Context
import android.content.SharedPreferences

/**
 * User Session Manager
 * Manages user session data throughout the app
 */
object UserSession {
    private const val PREF_NAME = "teamflow_session"
    private const val KEY_UID = "user_uid"
    private const val KEY_NAME = "user_name"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_PHOTO_URL = "user_photo_url"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_HAS_ACCOUNT = "has_account"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Save user session data
     */
    fun saveUser(context: Context, uid: String, name: String, email: String, photoUrl: String? = null) {
        getPrefs(context).edit().apply {
            putString(KEY_UID, uid)
            putString(KEY_NAME, name)
            putString(KEY_EMAIL, email)
            putString(KEY_PHOTO_URL, photoUrl)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_HAS_ACCOUNT, true)
            apply()
        }
    }
    
    /**
     * Get current user UID
     */
    fun getUid(context: Context): String? {
        return getPrefs(context).getString(KEY_UID, null)
    }
    
    /**
     * Get current user name
     */
    fun getName(context: Context): String? {
        return getPrefs(context).getString(KEY_NAME, null)
    }
    
    /**
     * Get current user email
     */
    fun getEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_EMAIL, null)
    }
    
    /**
     * Get current user photo URL
     */
    fun getPhotoUrl(context: Context): String? {
        return getPrefs(context).getString(KEY_PHOTO_URL, null)
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    /**
     * Check if account exists on device
     */
    fun hasAccount(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HAS_ACCOUNT, false)
    }
    
    /**
     * Mark account as existing on device
     */
    fun markAccountExists(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_HAS_ACCOUNT, true).apply()
    }
    
    /**
     * Clear user session (logout)
     */
    fun clearSession(context: Context) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_UID)
            remove(KEY_NAME)
            remove(KEY_EMAIL)
            remove(KEY_PHOTO_URL)
            apply()
        }
    }
    
    /**
     * Clear all data (for testing or account deletion)
     */
    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
