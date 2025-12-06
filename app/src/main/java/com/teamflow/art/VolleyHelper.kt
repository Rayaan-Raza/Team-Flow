package com.teamflow.art

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

/**
 * Singleton class for managing Volley request queue
 */
class VolleyHelper private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: VolleyHelper? = null
        
        fun getInstance(context: Context): VolleyHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolleyHelper(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(context.applicationContext)
    }
    
    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }
    
    fun cancelAll(tag: String) {
        requestQueue.cancelAll(tag)
    }
}
