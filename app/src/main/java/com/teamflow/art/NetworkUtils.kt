package com.teamflow.art

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Network utility to check internet connectivity and monitor changes
 */
object NetworkUtils {
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var wasOnline = true
    private val mainHandler = Handler(Looper.getMainLooper())
    
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo != null && networkInfo.isConnected
        }
    }
    
    /**
     * Start monitoring network connectivity changes
     * Shows toast when going offline/online and syncs when back online
     */
    fun startNetworkMonitoring(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // Check initial state
        wasOnline = isInternetAvailable(context)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    // Network is now available
                    if (!wasOnline) {
                        wasOnline = true
                        mainHandler.post {
                            Toast.makeText(context.applicationContext, "Back online - Syncing...", Toast.LENGTH_SHORT).show()
                        }
                        // Trigger sync
                        syncPendingData(context)
                    }
                }
                
                override fun onLost(network: Network) {
                    // Check if we still have any network
                    mainHandler.post {
                        if (!isInternetAvailable(context)) {
                            wasOnline = false
                            Toast.makeText(context.applicationContext, "Offline", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            try {
                connectivityManager.registerNetworkCallback(request, networkCallback!!)
            } catch (e: Exception) {
                android.util.Log.e("NetworkUtils", "Failed to register network callback", e)
            }
        }
    }
    
    /**
     * Stop monitoring network connectivity
     */
    fun stopNetworkMonitoring(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && networkCallback != null) {
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(networkCallback!!)
            } catch (e: Exception) {
                android.util.Log.e("NetworkUtils", "Failed to unregister network callback", e)
            }
            networkCallback = null
        }
    }
    
    /**
     * Sync pending operations when back online
     */
    private fun syncPendingData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val syncManager = SyncManager(context)
                syncManager.syncPendingOperations()
                
                mainHandler.post {
                    Toast.makeText(context.applicationContext, "Synced ✓", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("NetworkUtils", "Sync failed", e)
                mainHandler.post {
                    Toast.makeText(context.applicationContext, "Sync failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
