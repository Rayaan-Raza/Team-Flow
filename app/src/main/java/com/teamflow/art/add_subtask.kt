package com.teamflow.art

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

class add_subtask : AppCompatActivity() {

    private val dbRef = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private lateinit var btnBack: ImageView
    private lateinit var tvSubTitle: TextView
    private lateinit var tvSubHours: TextView
    private lateinit var btnMarkComplete: Button
    private lateinit var rvMembers: RecyclerView

    private lateinit var membersAdapter: ProjectMembersAdapter
    private val memberItems = mutableListOf<MemberItem>()

    private var projectId: String? = null
    private var taskId: String? = null
    private var subTaskId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_subtask)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        projectId = intent.getStringExtra("projectId")
        taskId = intent.getStringExtra("taskId")
        subTaskId = intent.getStringExtra("subTaskId")

        if (projectId.isNullOrBlank() || taskId.isNullOrBlank() || subTaskId.isNullOrBlank()) {
            Toast.makeText(this, "Missing ids", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        setupMembers()

        btnBack.setOnClickListener { finish() }
        btnMarkComplete.setOnClickListener { markSubTaskCompleteForMe() }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        tvSubTitle = findViewById(R.id.tvSubTitle)
        tvSubHours = findViewById(R.id.tvSubHours)
        btnMarkComplete = findViewById(R.id.btnMarkComplete)
        rvMembers = findViewById(R.id.rvMembers)
    }

    private fun setupMembers() {
        membersAdapter = ProjectMembersAdapter(memberItems)
        rvMembers.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvMembers.adapter = membersAdapter
    }

    private fun refresh() {
        val pid = projectId ?: return
        val tid = taskId ?: return
        val sid = subTaskId ?: return
        loadSubTask(pid, tid, sid)
    }

    private fun loadSubTask(pid: String, tid: String, sid: String) {
        dbRef.child("taskSubTasks").child(pid).child(tid).child(sid).get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    Toast.makeText(this, "Subtask not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val title = (snap.child("title").getValue(String::class.java) ?: "Untitled subtask").trim()
                tvSubTitle.text = title

                val hoursAny = snap.child("hours").value
                val hours = when (hoursAny) {
                    is Long -> hoursAny.toInt()
                    is Int -> hoursAny
                    is String -> hoursAny.toIntOrNull() ?: 0
                    else -> 0
                }
                tvSubHours.text = "${hours} hr"

                val status = (snap.child("status").getValue(String::class.java) ?: "in_progress").trim().lowercase()
                val isDone = (status == "done" || status == "completed")

                btnMarkComplete.isEnabled = !isDone
                btnMarkComplete.text = if (isDone) "Completed" else "Mark as complete"

                // show assignees on subtask (fallback to current user if missing)
                val assignees = snap.child("assignees").children.mapNotNull { it.key }.toSet()
                val uid = auth.currentUser?.uid
                val effectiveAssignees = if (assignees.isNotEmpty()) assignees else setOfNotNull(uid)

                loadMembersByUids(effectiveAssignees.toList())
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load subtask", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadMembersByUids(uids: List<String>) {
        if (uids.isEmpty()) {
            membersAdapter.setMembers(emptyList())
            return
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
                .addOnFailureListener {
                    temp.add(MemberItem(uid = uid))
                }
                .addOnCompleteListener {
                    remaining--
                    if (remaining == 0) {
                        val ordered = uids.map { id -> temp.firstOrNull { it.uid == id } ?: MemberItem(uid = id) }
                        membersAdapter.setMembers(ordered)
                    }
                }
        }
    }

    // ===================== COMPLETION LOGIC (SUBTASK) =====================

    private fun markSubTaskCompleteForMe() {
        val pid = projectId ?: return
        val tid = taskId ?: return
        val sid = subTaskId ?: return
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show()
            return
        }

        // mark MY click
        dbRef.child("taskSubTasks").child(pid).child(tid).child(sid).child("doneBy").child(uid).setValue(true)
            .addOnSuccessListener {
                attemptFinalizeSubtask(pid, tid, sid)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun attemptFinalizeSubtask(pid: String, tid: String, sid: String) {
        dbRef.child("taskSubTasks").child(pid).child(tid).child(sid).get()
            .addOnSuccessListener { subSnap ->

                val subAssignees = subSnap.child("assignees").children.mapNotNull { it.key }.toSet()
                val subDoneBy = subSnap.child("doneBy").children.mapNotNull { it.key }.toSet()

                // if subtask assignees missing => default to current user
                val uid = auth.currentUser?.uid
                val effectiveSubAssignees = if (subAssignees.isNotEmpty()) subAssignees else setOfNotNull(uid)

                val allClicked = effectiveSubAssignees.isNotEmpty() && effectiveSubAssignees.all { subDoneBy.contains(it) }

                if (allClicked) {
                    val now = System.currentTimeMillis()
                    val updates = hashMapOf<String, Any?>(
                        "taskSubTasks/$pid/$tid/$sid/status" to "done",
                        "taskSubTasks/$pid/$tid/$sid/updatedAt" to now
                    )

                    dbRef.updateChildren(updates)
                        .addOnSuccessListener {
                            // ✅ once a subtask becomes done, try to finalize the parent task too
                            attemptFinalizeParentTask(pid, tid)
                            refresh()
                            Toast.makeText(this, "Subtask completed", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    refresh()
                    Toast.makeText(this, "Marked by you. Waiting for others.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // ===================== FINALIZE PARENT TASK if possible =====================

    private fun attemptFinalizeParentTask(pid: String, tid: String) {
        dbRef.child("projectTasks").child(pid).child(tid).get()
            .addOnSuccessListener { taskSnap ->
                val taskAssignees = taskSnap.child("assignees").children.mapNotNull { it.key }.toSet()
                val taskDoneBy = taskSnap.child("doneBy").children.mapNotNull { it.key }.toSet()

                val currentUid = auth.currentUser?.uid
                val createdBy = taskSnap.child("createdBy").getValue(String::class.java)

                val effectiveTaskAssignees =
                    if (taskAssignees.isNotEmpty()) taskAssignees
                    else setOfNotNull(createdBy, currentUid)

                val allTaskAssigneesClicked = effectiveTaskAssignees.isNotEmpty() && effectiveTaskAssignees.all { taskDoneBy.contains(it) }

                dbRef.child("taskSubTasks").child(pid).child(tid).get()
                    .addOnSuccessListener { subsSnap ->
                        val allSubsDone = areAllSubtasksDone(subsSnap, effectiveTaskAssignees)

                        if (allTaskAssigneesClicked && allSubsDone) {
                            val now = System.currentTimeMillis()
                            val updates = hashMapOf<String, Any?>(
                                "projectTasks/$pid/$tid/status" to "done",
                                "projectTasks/$pid/$tid/updatedAt" to now
                            )
                            dbRef.updateChildren(updates).addOnSuccessListener {
                                recountProjectTotals(pid)
                            }
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
