package com.teamflow.art

import android.app.Activity
import android.content.Intent
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

/**
 * Bottom Navigation Helper
 * Manages bottom navigation state across all screens
 */
object BottomNavHelper {
    
    enum class NavItem {
        HOME, PROJECTS, CALENDAR, INBOX, PROFILE
    }
    
    /**
     * Setup bottom navigation for an activity
     * @param activity Current activity
     * @param currentItem Currently active navigation item
     */
    fun setupBottomNav(activity: Activity, currentItem: NavItem) {
        try {
            val navHome = activity.findViewById<LinearLayout>(R.id.navHome)
            val navProjects = activity.findViewById<LinearLayout>(R.id.navProjects)
            val navCalendar = activity.findViewById<LinearLayout>(R.id.navCalendar)
            val navInbox = activity.findViewById<LinearLayout>(R.id.navInbox)
            val navProfile = activity.findViewById<LinearLayout>(R.id.navProfile)
            
            // Try to find icon ImageViews (they may not exist in all layouts)
            val iconHome = navHome?.getChildAt(0) as? ImageView
            val iconProjects = navProjects?.getChildAt(0) as? ImageView
            val iconCalendar = navCalendar?.getChildAt(0) as? ImageView
            val iconInbox = navInbox?.getChildAt(0) as? ImageView
            val iconProfile = navProfile?.getChildAt(0) as? ImageView
            
            // Set all icons to grey first (if they exist)
            iconHome?.setColorFilter(ContextCompat.getColor(activity, android.R.color.darker_gray))
            iconProjects?.setColorFilter(ContextCompat.getColor(activity, android.R.color.darker_gray))
            iconCalendar?.setColorFilter(ContextCompat.getColor(activity, android.R.color.darker_gray))
            iconInbox?.setColorFilter(ContextCompat.getColor(activity, android.R.color.darker_gray))
            iconProfile?.setColorFilter(ContextCompat.getColor(activity, android.R.color.darker_gray))
            
            // Set current item to black
            when (currentItem) {
                NavItem.HOME -> iconHome?.setColorFilter(ContextCompat.getColor(activity, android.R.color.black))
                NavItem.PROJECTS -> iconProjects?.setColorFilter(ContextCompat.getColor(activity, android.R.color.black))
                NavItem.CALENDAR -> iconCalendar?.setColorFilter(ContextCompat.getColor(activity, android.R.color.black))
                NavItem.INBOX -> iconInbox?.setColorFilter(ContextCompat.getColor(activity, android.R.color.black))
                NavItem.PROFILE -> iconProfile?.setColorFilter(ContextCompat.getColor(activity, android.R.color.black))
            }
            
            // Set click listeners
            navHome?.setOnClickListener {
                if (currentItem != NavItem.HOME) {
                    activity.startActivity(Intent(activity, home_page::class.java))
                    activity.overridePendingTransition(0, 0)
                    activity.finish()
                }
            }
            
            navProjects?.setOnClickListener {
                if (currentItem != NavItem.PROJECTS) {
                    activity.startActivity(Intent(activity, project_list::class.java))
                    activity.overridePendingTransition(0, 0)
                    activity.finish()
                }
            }
            
            navCalendar?.setOnClickListener {
                if (currentItem != NavItem.CALENDAR) {
                    activity.startActivity(Intent(activity, calendar_screen::class.java))
                    activity.overridePendingTransition(0, 0)
                    activity.finish()
                }
            }
            
            navInbox?.setOnClickListener {
                if (currentItem != NavItem.INBOX) {
                    activity.startActivity(Intent(activity, inbox_all::class.java))
                    activity.overridePendingTransition(0, 0)
                    activity.finish()
                }
            }
            
            navProfile?.setOnClickListener {
                if (currentItem != NavItem.PROFILE) {
                    activity.startActivity(Intent(activity, profile_screen::class.java))
                    activity.overridePendingTransition(0, 0)
                    activity.finish()
                }
            }
        } catch (e: Exception) {
            // Silently fail if navigation views don't exist
            e.printStackTrace()
        }
    }
}
