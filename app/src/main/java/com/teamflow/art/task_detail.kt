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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task_detail)

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
            // Open user picker dialog for task assignment
            val pid = projectId ?: return@setOnClickListener
            val tid = taskId ?: return@setOnClickListener
            
            // Get current assignees
            dbRef.child("projectTasks").child(pid).child(tid).child("collaborators").get()
                .addOnSuccessListener { snap ->
                    val currentUids = snap.children.mapNotNull { it.key }
                    UserPickerDialog(this, currentUids) { selectedUsers ->
                        // Update task collaborators
                        val collaboratorsMap = selectedUsers.associate { it.uid!! to true }
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

        // ✅ click subtask -> add_subtask
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
        if (!NetworkUtils.isInternetAvailable(this)) {
            startActivity(Intent(this, No_Internet_Connection::class.java))
            overridePendingTransition(0, 0)
            return
        }
        val pid = projectId ?: return
        val tid = taskId ?: return
        loadTask(pid, tid)
        loadMembers(pid)
        loadSubTasks(pid, tid)
    }

    private fun loadTask(pid: String, tid: String) {
        dbRef.child("projectTasks").child(pid).child(tid).get()
            .addOnSuccessListener { snap ->
                val title = snap.child("title").getValue(String::class.java) ?: "Untitled task"
                val desc = snap.child("description").getValue(String::class.java) ?: ""
                val status = snap.child("status").getValue(String::class.java) ?: "in_progress"

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

    private fun loadMembers(pid: String) {
        dbRef.child("projectMembers").child(pid).get()
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
                    if (isDone) continue  // ✅ remove completed subtask from task_detail list

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
            startActivity(Intent(this, No_Internet_Connection::class.java))
            return
        }
        val pid = projectId ?: return
        val tid = taskId ?: return
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show()
            return
        }

        // mark MY done
        dbRef.child("projectTasks").child(pid).child(tid).child("doneBy").child(uid).setValue(true)
            .addOnSuccessListener {
                attemptFinalizeTask(pid, tid)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun attemptFinalizeTask(pid: String, tid: String) {
        // Need: task assignees all doneBy AND all subtasks done
        dbRef.child("projectTasks").child(pid).child(tid).get()
            .addOnSuccessListener { taskSnap ->
                val assignees = taskSnap.child("assignees").children.mapNotNull { it.key }.toSet()
                val doneBy = taskSnap.child("doneBy").children.mapNotNull { it.key }.toSet()

                val effectiveAssignees =
                    if (assignees.isNotEmpty()) assignees
                    else setOfNotNull(taskSnap.child("createdBy").getValue(String::class.java), auth.currentUser?.uid)

                val allAssigneesClicked = effectiveAssignees.all { doneBy.contains(it) }

                // block if any subtask incomplete
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
                            // keep in progress; just refresh so UI reflects "waiting"
                            dbRef.child("projectTasks").child(pid).child(tid).child("status").setValue("in_progress")
                            refreshAll()
                            Toast.makeText(this, "Marked by you. Waiting for others / subtasks.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
    }

    private fun areAllSubtasksDone(subsSnap: DataSnapshot, fallbackAssignees: Set<String>): Boolean {
        // If no subtasks => OK
        if (!subsSnap.exists()) return true

        for (s in subsSnap.children) {
            val status = (s.child("status").getValue(String::class.java) ?: "in_progress").lowercase()
            val isStatusDone = (status == "done" || status == "completed")
            if (isStatusDone) continue

            // if status not updated, compute from doneBy/assignees
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
