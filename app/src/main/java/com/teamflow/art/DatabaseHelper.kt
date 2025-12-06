package com.teamflow.art

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson

/**
 * SQLite Database Helper for offline data caching
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "teamflow_offline.db"
        private const val DATABASE_VERSION = 1
        
        // Table names
        const val TABLE_PROJECTS = "projects_cache"
        const val TABLE_TASKS = "tasks_cache"
        const val TABLE_SUBTASKS = "subtasks_cache"
        const val TABLE_MESSAGES = "messages_cache"
        const val TABLE_PENDING_OPS = "pending_operations"
        const val TABLE_SYNC_META = "sync_metadata"
        
        // Common columns
        const val COL_ID = "id"
        const val COL_FIREBASE_ID = "firebase_id"
        const val COL_DATA_JSON = "data_json"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_IS_SYNCED = "is_synced"
        
        // Pending operations columns
        const val COL_OPERATION_TYPE = "operation_type"  // "create", "update", "delete"
        const val COL_ENTITY_TYPE = "entity_type"  // "project", "task", "subtask", "message"
        const val COL_ENTITY_ID = "entity_id"
        const val COL_OPERATION_DATA = "operation_data"
        
        // Sync metadata columns
        const val COL_ENTITY_NAME = "entity_name"
        const val COL_LAST_SYNC = "last_sync"
    }
    
    private val gson = Gson()

    override fun onCreate(db: SQLiteDatabase) {
        // Projects cache table
        db.execSQL("""
            CREATE TABLE $TABLE_PROJECTS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FIREBASE_ID TEXT UNIQUE NOT NULL,
                $COL_DATA_JSON TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_IS_SYNCED INTEGER DEFAULT 1
            )
        """)
        
        // Tasks cache table
        db.execSQL("""
            CREATE TABLE $TABLE_TASKS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FIREBASE_ID TEXT UNIQUE NOT NULL,
                $COL_DATA_JSON TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_IS_SYNCED INTEGER DEFAULT 1
            )
        """)
        
        // SubTasks cache table
        db.execSQL("""
            CREATE TABLE $TABLE_SUBTASKS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FIREBASE_ID TEXT UNIQUE NOT NULL,
                $COL_DATA_JSON TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_IS_SYNCED INTEGER DEFAULT 1
            )
        """)
        
        // Messages cache table
        db.execSQL("""
            CREATE TABLE $TABLE_MESSAGES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FIREBASE_ID TEXT UNIQUE NOT NULL,
                $COL_DATA_JSON TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_IS_SYNCED INTEGER DEFAULT 1
            )
        """)
        
        // Pending operations table
        db.execSQL("""
            CREATE TABLE $TABLE_PENDING_OPS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_OPERATION_TYPE TEXT NOT NULL,
                $COL_ENTITY_TYPE TEXT NOT NULL,
                $COL_ENTITY_ID TEXT NOT NULL,
                $COL_OPERATION_DATA TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
        """)
        
        // Sync metadata table
        db.execSQL("""
            CREATE TABLE $TABLE_SYNC_META (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ENTITY_NAME TEXT UNIQUE NOT NULL,
                $COL_LAST_SYNC INTEGER NOT NULL
            )
        """)
        
        // Create indexes
        db.execSQL("CREATE INDEX idx_projects_firebase_id ON $TABLE_PROJECTS($COL_FIREBASE_ID)")
        db.execSQL("CREATE INDEX idx_tasks_firebase_id ON $TABLE_TASKS($COL_FIREBASE_ID)")
        db.execSQL("CREATE INDEX idx_subtasks_firebase_id ON $TABLE_SUBTASKS($COL_FIREBASE_ID)")
        db.execSQL("CREATE INDEX idx_messages_firebase_id ON $TABLE_MESSAGES($COL_FIREBASE_ID)")
        db.execSQL("CREATE INDEX idx_pending_ops_entity ON $TABLE_PENDING_OPS($COL_ENTITY_TYPE, $COL_ENTITY_ID)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop all tables and recreate
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROJECTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SUBTASKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING_OPS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SYNC_META")
        onCreate(db)
    }
    
    // Generic insert/update/delete methods
    fun insertOrUpdate(table: String, firebaseId: String, dataJson: String, isSynced: Boolean = true): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_FIREBASE_ID, firebaseId)
            put(COL_DATA_JSON, dataJson)
            put(COL_TIMESTAMP, System.currentTimeMillis())
            put(COL_IS_SYNCED, if (isSynced) 1 else 0)
        }
        
        return db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    fun getByFirebaseId(table: String, firebaseId: String): String? {
        val db = readableDatabase
        val cursor = db.query(
            table,
            arrayOf(COL_DATA_JSON),
            "$COL_FIREBASE_ID = ?",
            arrayOf(firebaseId),
            null, null, null
        )
        
        return cursor.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }
    
    fun getAll(table: String): List<String> {
        val db = readableDatabase
        val cursor = db.query(
            table,
            arrayOf(COL_DATA_JSON),
            null, null, null, null,
            "$COL_TIMESTAMP DESC"
        )
        
        val results = mutableListOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                results.add(it.getString(0))
            }
        }
        return results
    }
    
    fun deleteByFirebaseId(table: String, firebaseId: String): Int {
        val db = writableDatabase
        return db.delete(table, "$COL_FIREBASE_ID = ?", arrayOf(firebaseId))
    }
    
    // Pending operations
    fun addPendingOperation(operationType: String, entityType: String, entityId: String, operationData: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_OPERATION_TYPE, operationType)
            put(COL_ENTITY_TYPE, entityType)
            put(COL_ENTITY_ID, entityId)
            put(COL_OPERATION_DATA, operationData)
            put(COL_TIMESTAMP, System.currentTimeMillis())
        }
        return db.insert(TABLE_PENDING_OPS, null, values)
    }
    
    fun getPendingOperations(): List<PendingOperation> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PENDING_OPS,
            null, null, null, null, null,
            "$COL_TIMESTAMP ASC"
        )
        
        val operations = mutableListOf<PendingOperation>()
        cursor.use {
            while (it.moveToNext()) {
                operations.add(
                    PendingOperation(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        operationType = it.getString(it.getColumnIndexOrThrow(COL_OPERATION_TYPE)),
                        entityType = it.getString(it.getColumnIndexOrThrow(COL_ENTITY_TYPE)),
                        entityId = it.getString(it.getColumnIndexOrThrow(COL_ENTITY_ID)),
                        operationData = it.getString(it.getColumnIndexOrThrow(COL_OPERATION_DATA)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP))
                    )
                )
            }
        }
        return operations
    }
    
    fun deletePendingOperation(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_PENDING_OPS, "$COL_ID = ?", arrayOf(id.toString()))
    }
    
    // Sync metadata
    fun updateSyncTime(entityName: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ENTITY_NAME, entityName)
            put(COL_LAST_SYNC, System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_SYNC_META, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    fun getLastSyncTime(entityName: String): Long {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SYNC_META,
            arrayOf(COL_LAST_SYNC),
            "$COL_ENTITY_NAME = ?",
            arrayOf(entityName),
            null, null, null
        )
        
        return cursor.use {
            if (it.moveToFirst()) it.getLong(0) else 0L
        }
    }
    
    fun clearAll() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_PROJECTS")
        db.execSQL("DELETE FROM $TABLE_TASKS")
        db.execSQL("DELETE FROM $TABLE_SUBTASKS")
        db.execSQL("DELETE FROM $TABLE_MESSAGES")
        db.execSQL("DELETE FROM $TABLE_PENDING_OPS")
        db.execSQL("DELETE FROM $TABLE_SYNC_META")
    }
}

data class PendingOperation(
    val id: Long,
    val operationType: String,
    val entityType: String,
    val entityId: String,
    val operationData: String,
    val timestamp: Long
)
