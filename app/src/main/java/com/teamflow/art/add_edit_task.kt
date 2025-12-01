package com.teamflow.art

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.EditText
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
import java.util.Calendar

class add_edit_task : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val dbRef = FirebaseDatabase.getInstance().reference

    private lateinit var btnBack: ImageView
    private lateinit var btnSave: ImageView

    private lateinit var etTitle: EditText
    private lateinit var etDesc: EditText
    private lateinit var tvDueDate: TextView

    private lateinit var rvMembers: RecyclerView
    private lateinit var btnAddAssignee: ImageView

    private lateinit var rvSubTasks: RecyclerView
    private lateinit var btnAddSubTask: ImageView

    private lateinit var membersAdapter: ProjectMembersAdapter
    private lateinit var subTasksAdapter: SubTasksDraftAdapter

    private val memberItems = mutableListOf<MemberItem>()
    private val subDrafts = mutableListOf<SubTaskDraft>()

    private var dueAtMillis: Long? = null

    private var projectId: String? = null
    private var taskId: String? = null
    private var isEditMode: Boolean = false
    private var currentUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_edit_task)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        currentUid = auth.currentUser?.uid

        projectId = intent.getStringExtra("projectId")
        taskId = intent.getStringExtra("taskId")
        isEditMode = !taskId.isNullOrBlank()

        if (projectId.isNullOrBlank()) {
            Toast.makeText(this, "Missing project id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        setupLists()

        btnBack.setOnClickListener { finish() }
        tvDueDate.setOnClickListener { openDatePicker() }
        btnAddAssignee.setOnClickListener { Toast.makeText(this, "Assign/edit later", Toast.LENGTH_SHORT).show() }
        btnAddSubTask.setOnClickListener { subTasksAdapter.addEmpty() }

        btnSave.setOnClickListener {
            if (isEditMode) updateTask(projectId!!, taskId!!) else createTask(projectId!!)
        }

        loadMembers(projectId!!)

        if (isEditMode) {
            loadTaskForEdit(projectId!!, taskId!!)
            loadSubTasksForEdit(projectId!!, taskId!!)
        } else {
            subDrafts.clear()
            subDrafts.add(SubTaskDraft())
            subTasksAdapter.setItems(subDrafts)
        }
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSave)

        etTitle = findViewById(R.id.etTaskTitle)
        etDesc = findViewById(R.id.etTaskDesc)
        tvDueDate = findViewById(R.id.tvDueDate)

        rvMembers = findViewById(R.id.rvMembers)
        btnAddAssignee = findViewById(R.id.btnAddAssignee)

        rvSubTasks = findViewById(R.id.rvSubTasks)
        btnAddSubTask = findViewById(R.id.btnAddSubTask)
    }

    private fun setupLists() {
        membersAdapter = ProjectMembersAdapter(memberItems)
        rvMembers.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvMembers.adapter = membersAdapter

        subTasksAdapter = SubTasksDraftAdapter(subDrafts)
        rvSubTasks.layoutManager = LinearLayoutManager(this)
        rvSubTasks.adapter = subTasksAdapter
    }

    private fun openDatePicker() {
        val cal = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                val c = Calendar.getInstance()
                c.set(Calendar.YEAR, year)
                c.set(Calendar.MONTH, month)
                c.set(Calendar.DAY_OF_MONTH, day)
                c.set(Calendar.HOUR_OF_DAY, 0)
                c.set(Calendar.MINUTE, 0)
                c.set(Calendar.SECOND, 0)
                c.set(Calendar.MILLISECOND, 0)

                dueAtMillis = c.timeInMillis
                tvDueDate.text = "Due date: $day/${month + 1}/$year"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
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
                                val ordered = uids.map { id ->
                                    temp.firstOrNull { it.uid == id } ?: MemberItem(uid = id)
                                }
                                membersAdapter.setMembers(ordered)
                            }
                        }
                }
            }
    }

    private fun loadTaskForEdit(pid: String, tid: String) {
        dbRef.child("projectTasks").child(pid).child(tid).get()
            .addOnSuccessListener { snap ->
                etTitle.setText(snap.child("title").getValue(String::class.java) ?: "")
                etDesc.setText(snap.child("description").getValue(String::class.java) ?: "")

                val dueAny = snap.child("dueAt").value
                dueAtMillis = when (dueAny) {
                    is Long -> dueAny
                    is Int -> dueAny.toLong()
                    is String -> dueAny.toLongOrNull()
                    else -> null
                }

                tvDueDate.text = if (dueAtMillis != null) "Due date: (tap to change)" else "Due date: Not set"
            }
    }

    private fun loadSubTasksForEdit(pid: String, tid: String) {
        dbRef.child("taskSubTasks").child(pid).child(tid).get()
            .addOnSuccessListener { snap ->
                val list = mutableListOf<SubTaskDraft>()
                for (s in snap.children) {
                    val id = s.key ?: continue
                    val title = (s.child("title").getValue(String::class.java) ?: "").trim()
                    val status = (s.child("status").getValue(String::class.java) ?: "in_progress").trim()

                    val hoursAny = s.child("hours").value
                    val hours = when (hoursAny) {
                        is Long -> hoursAny.toInt()
                        is Int -> hoursAny
                        is String -> hoursAny.toIntOrNull() ?: 0
                        else -> 0
                    }

                    list.add(SubTaskDraft(id = id, title = title, hours = hours, status = status))
                }

                if (list.isEmpty()) list.add(SubTaskDraft())
                subTasksAdapter.setItems(list)
            }
    }

    private fun createTask(pid: String) {
        val uid = currentUid ?: run {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show()
            return
        }

        val title = etTitle.text.toString().trim()
        val desc = etDesc.text.toString().trim()

        if (title.isEmpty()) {
            etTitle.error = "Required"
            etTitle.requestFocus()
            return
        }

        val tid = dbRef.child("projectTasks").child(pid).push().key ?: run {
            Toast.makeText(this, "Failed to create task", Toast.LENGTH_SHORT).show()
            return
        }

        val now = System.currentTimeMillis()

        // Fetch project members -> use them as task assignees
        dbRef.child("projectMembers").child(pid).get()
            .addOnSuccessListener { memSnap ->
                val memberUids = memSnap.children.mapNotNull { it.key }.toMutableSet()

                // safety: never allow "0 assignees" (would block completion)
                if (memberUids.isEmpty()) memberUids.add(uid)

                val updates = hashMapOf<String, Any?>()

                // main task
                updates["projectTasks/$pid/$tid"] = mapOf(
                    "id" to tid,
                    "projectId" to pid,
                    "title" to title,
                    "description" to desc,
                    "status" to "in_progress",
                    "dueAt" to dueAtMillis,
                    "createdBy" to uid,
                    "createdAt" to now,
                    "updatedAt" to now
                )

                // assignees map
                for (auid in memberUids) {
                    updates["projectTasks/$pid/$tid/assignees/$auid"] = true
                }

                // doneBy should start empty (don’t write anything here)

                // subtasks
                val subs = subTasksAdapter.getItems().filter { it.title.isNotBlank() }
                for (s in subs) {
                    val sid = dbRef.child("taskSubTasks").child(pid).child(tid).push().key ?: continue
                    updates["taskSubTasks/$pid/$tid/$sid"] = mapOf(
                        "id" to sid,
                        "projectId" to pid,
                        "taskId" to tid,
                        "title" to s.title.trim(),
                        "hours" to s.hours,
                        "status" to s.status,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                }

                dbRef.updateChildren(updates)
                    .addOnSuccessListener {
                        recountProjectTotals(pid)
                        Toast.makeText(this, "Task created", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to read project members", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTask(pid: String, tid: String) {
        val uid = currentUid ?: ""

        val title = etTitle.text.toString().trim()
        val desc = etDesc.text.toString().trim()

        if (title.isEmpty()) {
            etTitle.error = "Required"
            etTitle.requestFocus()
            return
        }

        val now = System.currentTimeMillis()

        dbRef.child("taskSubTasks").child(pid).child(tid).get()
            .addOnSuccessListener { existingSnap ->

                val existingIds = existingSnap.children.mapNotNull { it.key }.toSet()
                val presentIds = mutableSetOf<String>()
                val updates = hashMapOf<String, Any?>()

                updates["projectTasks/$pid/$tid/title"] = title
                updates["projectTasks/$pid/$tid/description"] = desc
                updates["projectTasks/$pid/$tid/dueAt"] = dueAtMillis
                updates["projectTasks/$pid/$tid/updatedAt"] = now
                updates["projectTasks/$pid/$tid/editedBy"] = uid

                for (s in subTasksAdapter.getItems()) {
                    val st = s.title.trim()
                    if (st.isBlank()) {
                        if (!s.id.isNullOrBlank()) updates["taskSubTasks/$pid/$tid/${s.id}"] = null
                        continue
                    }

                    val sid = if (s.id.isNullOrBlank()) {
                        dbRef.child("taskSubTasks").child(pid).child(tid).push().key ?: continue
                    } else s.id!!

                    s.id = sid
                    presentIds.add(sid)

                    updates["taskSubTasks/$pid/$tid/$sid"] = mapOf(
                        "id" to sid,
                        "projectId" to pid,
                        "taskId" to tid,
                        "title" to st,
                        "hours" to s.hours,
                        "status" to s.status,
                        "updatedAt" to now
                    )
                }

                for (old in existingIds) {
                    if (!presentIds.contains(old)) updates["taskSubTasks/$pid/$tid/$old"] = null
                }

                dbRef.updateChildren(updates)
                    .addOnSuccessListener {
                        recountProjectTotals(pid)
                        Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show() }
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
