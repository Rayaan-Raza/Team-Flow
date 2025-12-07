package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class task_detail : AppCompatActivity() {

    private val dbRef = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private lateinit var btnBack: ImageView
    private lateinit var btnEdit: ImageView
    private lateinit var btnEditSubTasks: ImageView
    private lateinit var btnAddAssignee: ImageView

    private lateinit var tvTitle: TextView
    private lateinit var tvDesc: TextView
    private lateinit var tvDue: TextView
    private lateinit var btnMarkDone: Button

    private lateinit var rvMembers: RecyclerView
    private lateinit var rvSubTasks: RecyclerView

    private lateinit var membersAdapter: ProjectMembersAdapter
    private lateinit var subTasksAdapter: ProjectDetailTasksAdapter

    private val memberItems = mutableListOf<MemberItem>()
    private val subTaskItems = mutableListOf<ProjectTaskItem>()

    private var projectId: String? = null
    private var taskId: String? = null
    
    // SQLite support
    private lateinit var dbHelper: DatabaseHelper
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task_detail)
        
        // Initialize SQLite
        dbHelper = DatabaseHelper(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        projectId = intent.getStringExtra("projectId")
        taskId = intent.getStringExtra("taskId")

        if (projectId.isNullOrBlank() || taskId.isNullOrBlank()) {
            Toast.makeText(this, "Missing project/task id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        setupLists()

        btnBack.setOnClickListener { finish() }

        btnEdit.setOnClickListener {
            val i = Intent(this, add_edit_task::class.java)
            i.putExtra("projectId", projectId)
            i.putExtra("taskId", taskId)
            startActivity(i)
            overridePendingTransition(0, 0)
        }

        btnEditSubTasks.setOnClickListener {
            val i = Intent(this, add_edit_task::class.java)
            i.putExtra("projectId", projectId)
            i.putExtra("taskId", taskId)
            startActivity(i)
            overridePendingTransition(0, 0)
        }

        btnAddAssignee.setOnClickListener {
            if (!NetworkUtils.isInternetAvailable(this)) {
                Toast.makeText(this, "Network required to update assignees", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val pid = projectId ?: return@setOnClickListener
            val tid = taskId ?: return@setOnClickListener
            
            dbRef.child("projectTasks").child(pid).child(tid).get()
                .addOnSuccessListener { taskSnap ->
                    val createdBy = taskSnap.child("createdBy").getValue(String::class.java)
                    val currentUids = taskSnap.child("collaborators").children.mapNotNull { it.key }
                    
                    UserPickerDialog(this, currentUids) { selectedUsers ->
                        val collaboratorsMap = selectedUsers.associate { it.uid!! to true }.toMutableMap()
                        if (createdBy != null && !collaboratorsMap.containsKey(createdBy)) {
                            collaboratorsMap[createdBy] = true
                        }
                        
                        dbRef.child("projectTasks").child(pid).child(tid).child("collaborators")
                            .setValue(collaboratorsMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Assignees updated", Toast.LENGTH_SHORT).show()
                                refreshAll()
                            }
                    }.show()
                }
        }

        btnMarkDone.setOnClickListener { markTaskDoneForMe() }

        refreshAll()
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        btnEdit = findViewById(R.id.btnEdit)
        btnEditSubTasks = findViewById(R.id.btnEditSubTasks)
        btnAddAssignee = findViewById(R.id.btnAddAssignee)

        tvTitle = findViewById(R.id.taskTitle)
        tvDesc = findViewById(R.id.taskDesc)
        tvDue = findViewById(R.id.taskDue)
        btnMarkDone = findViewById(R.id.btnMarkDone)

        rvMembers = findViewById(R.id.rvMembers)
        rvSubTasks = findViewById(R.id.rvSubTasks)
    }

    private fun setupLists() {
        membersAdapter = ProjectMembersAdapter(memberItems)
        rvMembers.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvMembers.adapter = membersAdapter

        subTasksAdapter = ProjectDetailTasksAdapter(subTaskItems) { sub ->
            val pid = projectId ?: return@ProjectDetailTasksAdapter
            val tid = taskId ?: return@ProjectDetailTasksAdapter
            val i = Intent(this, add_subtask::class.java)
            i.putExtra("projectId", pid)
            i.putExtra("taskId", tid)
            i.putExtra("subTaskId", sub.id)
            startActivity(i)
            overridePendingTransition(0, 0)
        }

        rvSubTasks.layoutManager = LinearLayoutManager(this)
        rvSubTasks.adapter = subTasksAdapter
    }

    private fun refreshAll() {
        val pid = projectId ?: return
        val tid = taskId ?: return
        
        if (NetworkUtils.isInternetAvailable(this)) {
            // Online: load from Firebase and cache
            loadTask(pid, tid)
            loadMembers(pid, tid)
            loadSubTasks(pid, tid)
        } else {
            // Offline: load from cache
            loadTaskFromCache(tid)
            loadSubTasksFromCache(tid)
            Toast.makeText(this, "Viewing offline data", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadTaskFromCache(tid: String) {
        val json = dbHelper.getByFirebaseId(DatabaseHelper.TABLE_TASKS, tid) ?: return
        val task = gson.fromJson(json, Task::class.java) ?: return
        
        tvTitle.text = task.title ?: "Untitled task"
        tvDesc.text = task.description ?: ""
        
        val due = task.dueAt
        if (due != null) {
            val fmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
            tvDue.text = "Due date: ${fmt.format(Date(due))}"
        } else {
            tvDue.text = "Due date: Not set"
        }
        
        val s = (task.status ?: "in_progress").lowercase()
        val isDone = (s == "done" || s == "completed")
        btnMarkDone.isEnabled = !isDone
        btnMarkDone.text = if (isDone) "Completed" else "Mark as done"
    }
    
    private fun loadSubTasksFromCache(tid: String) {
        // Get all cached subtasks and filter by taskId
        val allSubtasks = dbHelper.getAll(DatabaseHelper.TABLE_SUBTASKS)
        val list = mutableListOf<ProjectTaskItem>()
        
        for (json in allSubtasks) {
            try {
                val subtask = gson.fromJson(json, SubTask::class.java)
                if (subtask.taskId == tid && subtask.status != "done" && subtask.status != "completed") {
                    list.add(ProjectTaskItem(
                        id = subtask.id ?: "",
                        title = subtask.title ?: "",
                        hours = subtask.hours ?: 0,
                        status = subtask.status ?: "in_progress"
                    ))
                }
            } catch (e: Exception) { }
        }
        
        subTasksAdapter.setTasks(list)
    }

    private fun loadTask(pid: String, tid: String) {
        dbRef.child("projectTasks").child(pid).child(tid).get()
            .addOnSuccessListener { snap ->
                val title = snap.child("title").getValue(String::class.java) ?: "Untitled task"
                val desc = snap.child("description").getValue(String::class.java) ?: ""
                val status = snap.child("status").getValue(String::class.java) ?: "in_progress"

                // Cache to SQLite
                val task = Task(
                    id = tid,
                    projectId = pid,
                    title = title,
                    description = desc,
                    status = status,
                    dueAt = (snap.child("dueAt").value as? Long)
                )
                dbHelper.insertOrUpdate(DatabaseHelper.TABLE_TASKS, tid, gson.toJson(task), true)

                tvTitle.text = title
                tvDesc.text = desc

                val dueAny = snap.child("dueAt").value
                val due = when (dueAny) {
                    is Long -> dueAny
                    is Int -> dueAny.toLong()
                    else -> null
                }

                if (due != null) {
                    val fmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
                    tvDue.text = "Due date: ${fmt.format(Date(due))}"
                } else tvDue.text = "Due date: Not set"

                val s = status.lowercase()
                val isDone = (s == "done" || s == "completed")
                btnMarkDone.isEnabled = !isDone
                btnMarkDone.text = if (isDone) "Completed" else "Mark as done"
            }
    }

    private fun loadMembers(pid: String, tid: String) {
        dbRef.child("projectTasks").child(pid).child(tid).child("collaborators").get()
            .addOnSuccessListener { snap ->
                val uids = snap.children.mapNotNull { it.key }
                
                if (uids.isEmpty()) {
                    membersAdapter.setMembers(emptyList())
                    return@addOnSuccessListener
                }

                val temp = mutableListOf<MemberItem>()
                var remaining = uids.size

                for (uid in uids) {
                    dbRef.child("users").child(uid).get()
                        .addOnSuccessListener { uSnap ->
                            val name = uSnap.child("name").getValue(String::class.java) ?: ""
                            val photo = uSnap.child("photoBase64").getValue(String::class.java)
                            temp.add(MemberItem(uid = uid, name = name, photoBase64 = photo))
                        }
                        .addOnFailureListener { temp.add(MemberItem(uid = uid)) }
                        .addOnCompleteListener {
                            remaining--
                            if (remaining == 0) {
                                val ordered = uids.map { id -> temp.firstOrNull { it.uid == id } ?: MemberItem(uid = id) }
                                membersAdapter.setMembers(ordered)
                            }
                        }
                }
            }
            .addOnFailureListener { membersAdapter.setMembers(emptyList()) }
    }

    private fun loadSubTasks(pid: String, tid: String) {
        dbRef.child("taskSubTasks").child(pid).child(tid).get()
            .addOnSuccessListener { snap ->
                val list = mutableListOf<ProjectTaskItem>()
                for (s in snap.children) {
                    val id = s.key ?: continue
                    val title = (s.child("title").getValue(String::class.java) ?: "").trim()
                    if (title.isBlank()) continue

                    val status = (s.child("status").getValue(String::class.java) ?: "in_progress").trim()
                    val sLower = status.lowercase()
                    val isDone = (sLower == "done" || sLower == "completed")
                    
                    // Cache subtask to SQLite
                    val subtask = SubTask(
                        id = id,
                        taskId = tid,
                        title = title,
                        status = status,
                        hours = (s.child("hours").value as? Long)?.toInt() ?: 0
                    )
                    dbHelper.insertOrUpdate(DatabaseHelper.TABLE_SUBTASKS, id, gson.toJson(subtask), true)
                    
                    if (isDone) continue

                    val hoursAny = s.child("hours").value
                    val hours = when (hoursAny) {
                        is Long -> hoursAny.toInt()
                        is Int -> hoursAny
                        is String -> hoursAny.toIntOrNull() ?: 0
                        else -> 0
                    }

                    list.add(ProjectTaskItem(id = id, title = title, hours = hours, status = status))
                }
                subTasksAdapter.setTasks(list)
            }
    }

    // ===================== COMPLETION LOGIC =====================

    private fun markTaskDoneForMe() {
        if (!NetworkUtils.isInternetAvailable(this)) {
            Toast.makeText(this, "Network required to mark as done", Toast.LENGTH_SHORT).show()
            return
        }
        val pid = projectId ?: return
        val tid = taskId ?: return
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show()
            return
        }

        btnMarkDone.isEnabled = false
        btnMarkDone.text = "Marked"

        dbRef.child("projectTasks").child(pid).child(tid).child("doneBy").child(uid).setValue(true)
            .addOnSuccessListener {
                sendTaskCompletionNotifications(pid, tid, uid)
                attemptFinalizeTask(pid, tid)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun sendTaskCompletionNotifications(pid: String, tid: String, senderUid: String) {
        dbRef.child("users").child(senderUid).child("name").get()
            .addOnSuccessListener { nameSnap ->
                val senderName = nameSnap.getValue(String::class.java) ?: "Someone"
                
                dbRef.child("projectTasks").child(pid).child(tid).get()
                    .addOnSuccessListener { taskSnap ->
                        val taskTitle = taskSnap.child("title").getValue(String::class.java) ?: "a task"
                        val collaborators = taskSnap.child("collaborators").children.mapNotNull { it.key }.toSet()
                        
                        for (recipientUid in collaborators) {
                            if (recipientUid != senderUid) {
                                createNotification(
                                    recipientUid = recipientUid,
                                    type = "task_completed",
                                    title = "Task Progress Update",
                                    body = "$senderName marked \"$taskTitle\" as done",
                                    senderUid = senderUid,
                                    senderName = senderName,
                                    projectId = pid,
                                    taskId = tid
                                )
                            }
                        }
                    }
            }
    }
    
    private fun createNotification(
        recipientUid: String,
        type: String,
        title: String,
        body: String,
        senderUid: String,
        senderName: String,
        projectId: String,
        taskId: String,
        subTaskId: String? = null
    ) {
        val notifRef = dbRef.child("notifications").child(recipientUid).push()
        val notifId = notifRef.key ?: return
        
        val notification = mapOf(
            "id" to notifId,
            "uid" to recipientUid,
            "type" to type,
            "title" to title,
            "body" to body,
            "senderUid" to senderUid,
            "senderName" to senderName,
            "projectId" to projectId,
            "taskId" to taskId,
            "subTaskId" to subTaskId,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )
        
        notifRef.setValue(notification)
    }

    private fun attemptFinalizeTask(pid: String, tid: String) {
        dbRef.child("projectTasks").child(pid).child(tid).get()
            .addOnSuccessListener { taskSnap ->
                val assignees = taskSnap.child("assignees").children.mapNotNull { it.key }.toSet()
                val doneBy = taskSnap.child("doneBy").children.mapNotNull { it.key }.toSet()

                val effectiveAssignees =
                    if (assignees.isNotEmpty()) assignees
                    else setOfNotNull(taskSnap.child("createdBy").getValue(String::class.java), auth.currentUser?.uid)

                val allAssigneesClicked = effectiveAssignees.all { doneBy.contains(it) }

                dbRef.child("taskSubTasks").child(pid).child(tid).get()
                    .addOnSuccessListener { subsSnap ->
                        val allSubTasksDone = areAllSubtasksDone(subsSnap, effectiveAssignees)

                        val now = System.currentTimeMillis()
                        if (allAssigneesClicked && allSubTasksDone) {
                            val updates = hashMapOf<String, Any?>(
                                "projectTasks/$pid/$tid/status" to "done",
                                "projectTasks/$pid/$tid/updatedAt" to now
                            )
                            dbRef.updateChildren(updates).addOnSuccessListener {
                                recountProjectTotals(pid)
                                refreshAll()
                                Toast.makeText(this, "Task completed", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            dbRef.child("projectTasks").child(pid).child(tid).child("status").setValue("in_progress")
                            refreshAll()
                            Toast.makeText(this, "Marked by you. Waiting for others / subtasks.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
    }

    private fun areAllSubtasksDone(subsSnap: DataSnapshot, fallbackAssignees: Set<String>): Boolean {
        if (!subsSnap.exists()) return true

        for (s in subsSnap.children) {
            val status = (s.child("status").getValue(String::class.java) ?: "in_progress").lowercase()
            val isStatusDone = (status == "done" || status == "completed")
            if (isStatusDone) continue

            val subAssignees = s.child("assignees").children.mapNotNull { it.key }.toSet()
            val subDoneBy = s.child("doneBy").children.mapNotNull { it.key }.toSet()
            val effective = if (subAssignees.isNotEmpty()) subAssignees else fallbackAssignees

            val allClicked = effective.isNotEmpty() && effective.all { subDoneBy.contains(it) }
            if (!allClicked) return false
        }
        return true
    }

    private fun recountProjectTotals(pid: String) {
        dbRef.child("projectTasks").child(pid).get()
            .addOnSuccessListener { snap ->
                var total = 0
                var doneCount = 0

                for (t in snap.children) {
                    val title = (t.child("title").getValue(String::class.java) ?: "").trim()
                    if (title.isBlank()) continue

                    total++
                    val s = (t.child("status").getValue(String::class.java) ?: "in_progress").lowercase()
                    if (s == "done" || s == "completed") doneCount++
                }

                val projectStatus = if (total > 0 && doneCount == total) "completed" else "in_progress"

                val u = hashMapOf<String, Any?>(
                    "projects/$pid/tasksTotal" to total,
                    "projects/$pid/tasksDone" to doneCount,
                    "projects/$pid/status" to projectStatus,
                    "projects/$pid/updatedAt" to System.currentTimeMillis()
                )

                dbRef.updateChildren(u)
            }
    }
}
