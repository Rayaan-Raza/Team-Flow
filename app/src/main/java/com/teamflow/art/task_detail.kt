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
import com.google.firebase.database.FirebaseDatabase
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

    private var currentUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        currentUid = auth.currentUser?.uid

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
        }

        btnEditSubTasks.setOnClickListener {
            val i = Intent(this, add_edit_task::class.java)
            i.putExtra("projectId", projectId)
            i.putExtra("taskId", taskId)
            startActivity(i)
        }

        btnAddAssignee.setOnClickListener {
            Toast.makeText(this, "Assignee editing later", Toast.LENGTH_SHORT).show()
        }

        btnMarkDone.setOnClickListener { onMarkDoneClicked() }

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

        subTasksAdapter = ProjectDetailTasksAdapter(subTaskItems) { /* no-op click */ }
        rvSubTasks.layoutManager = LinearLayoutManager(this)
        rvSubTasks.adapter = subTasksAdapter
    }

    private fun refreshAll() {
        val pid = projectId ?: return
        val tid = taskId ?: return

        loadTask(pid, tid)
        loadTaskAssignees(pid, tid)
        loadSubTasks(pid, tid)
    }

    private fun loadTask(pid: String, tid: String) {
        dbRef.child("projectTasks").child(pid).child(tid).get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val title = snap.child("title").getValue(String::class.java) ?: "Untitled task"
                val desc = snap.child("description").getValue(String::class.java) ?: ""
                val statusRaw = (snap.child("status").getValue(String::class.java) ?: "in_progress").trim()
                val status = statusRaw.lowercase()

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
                } else {
                    tvDue.text = "Due date: Not set"
                }

                val uid = currentUid
                val isTaskDone = (status == "done" || status == "completed")

                // per-user done state
                val iMarkedDone = uid != null && snap.child("doneBy").child(uid).exists()

                if (isTaskDone) {
                    btnMarkDone.isEnabled = false
                    btnMarkDone.text = "Completed"
                } else {
                    if (iMarkedDone) {
                        btnMarkDone.isEnabled = false
                        btnMarkDone.text = "Marked done (waiting)"
                    } else {
                        btnMarkDone.isEnabled = true
                        btnMarkDone.text = "Mark as done"
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load task", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Shows assignees for THIS TASK (not project members).
     * Falls back to project members if task has no assignees yet.
     */
    private fun loadTaskAssignees(pid: String, tid: String) {
        dbRef.child("projectTasks").child(pid).child(tid).child("assignees").get()
            .addOnSuccessListener { snap ->
                val uids = snap.children.mapNotNull { it.key }

                if (uids.isEmpty()) {
                    // fallback for older tasks (no assignees stored yet)
                    loadProjectMembers(pid)
                    return@addOnSuccessListener
                }

                enrichUsersToMembers(uids) { membersAdapter.setMembers(it) }
            }
            .addOnFailureListener {
                loadProjectMembers(pid)
            }
    }

    private fun loadProjectMembers(pid: String) {
        dbRef.child("projectMembers").child(pid).get()
            .addOnSuccessListener { snap ->
                val uids = snap.children.mapNotNull { it.key }
                if (uids.isEmpty()) {
                    membersAdapter.setMembers(emptyList())
                    return@addOnSuccessListener
                }
                enrichUsersToMembers(uids) { membersAdapter.setMembers(it) }
            }
            .addOnFailureListener {
                membersAdapter.setMembers(emptyList())
            }
    }

    private fun enrichUsersToMembers(uids: List<String>, onDone: (List<MemberItem>) -> Unit) {
        val temp = mutableListOf<MemberItem>()
        var remaining = uids.size

        for (uid in uids) {
            dbRef.child("users").child(uid).get()
                .addOnSuccessListener { uSnap ->
                    val name = uSnap.child("name").getValue(String::class.java) ?: ""
                    val photo = uSnap.child("photoBase64").getValue(String::class.java)
                    temp.add(MemberItem(uid = uid, name = name, photoBase64 = photo))
                }
                .addOnFailureListener {
                    temp.add(MemberItem(uid = uid))
                }
                .addOnCompleteListener {
                    remaining--
                    if (remaining == 0) {
                        val ordered = uids.map { id -> temp.firstOrNull { it.uid == id } ?: MemberItem(uid = id) }
                        onDone(ordered)
                    }
                }
        }
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

    private fun onMarkDoneClicked() {
        val pid = projectId ?: return
        val tid = taskId ?: return
        val uid = currentUid

        if (uid.isNullOrBlank()) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show()
            return
        }

        val now = System.currentTimeMillis()

        // Step 1) mark THIS user as doneBy
        val updates = hashMapOf<String, Any?>(
            "projectTasks/$pid/$tid/doneBy/$uid" to true,
            "projectTasks/$pid/$tid/updatedAt" to now
        )

        dbRef.updateChildren(updates)
            .addOnSuccessListener {
                // Step 2) check if everyone is done, then finalize status if yes
                checkAndFinalizeTask(pid, tid)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkAndFinalizeTask(pid: String, tid: String) {
        dbRef.child("projectTasks").child(pid).child(tid).get()
            .addOnSuccessListener { snap ->
                // if assignees is missing, fallback to "only current user" to avoid blocking
                val assignees = snap.child("assignees").children.mapNotNull { it.key }.toSet()
                val doneBy = snap.child("doneBy").children.mapNotNull { it.key }.toSet()

                val uid = currentUid
                val effectiveAssignees =
                    if (assignees.isNotEmpty()) assignees
                    else if (!uid.isNullOrBlank()) setOf(uid)
                    else emptySet()

                // ensure the fallback assignee exists in DB too (optional but helps consistency)
                if (assignees.isEmpty() && !uid.isNullOrBlank()) {
                    dbRef.child("projectTasks").child(pid).child(tid).child("assignees").child(uid).setValue(true)
                }

                val allDone = effectiveAssignees.isNotEmpty() && doneBy.containsAll(effectiveAssignees)

                if (allDone) {
                    val now = System.currentTimeMillis()
                    val finalize = hashMapOf<String, Any?>(
                        "projectTasks/$pid/$tid/status" to "done",
                        "projectTasks/$pid/$tid/updatedAt" to now
                    )

                    dbRef.updateChildren(finalize)
                        .addOnSuccessListener {
                            recountProjectTotals(pid)
                            refreshAll()
                            Toast.makeText(this, "Task completed", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // not all done yet, stay in_progress
                    refreshAll()
                    Toast.makeText(this, "Marked done (waiting others)", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to verify completion", Toast.LENGTH_SHORT).show()
            }
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
