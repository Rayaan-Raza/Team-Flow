package com.teamflow.art

import android.app.DatePickerDialog
import android.content.Intent
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

class add_edit_project : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val dbRef = FirebaseDatabase.getInstance().reference

    private lateinit var btnBack: ImageView
    private lateinit var btnSave: ImageView

    private lateinit var etTitle: EditText
    private lateinit var etDesc: EditText
    private lateinit var tvDueDate: TextView

    private lateinit var rvAssignees: RecyclerView
    private lateinit var btnAddAssignee: ImageView

    private lateinit var rvTasks: RecyclerView
    private lateinit var btnAddTask: ImageView

    private lateinit var assigneesAdapter: AssigneesAdapter
    private lateinit var tasksAdapter: ProjectTasksDraftAdapter

    private val assignees = mutableListOf<AssigneeDraft>()
    private val taskDrafts = mutableListOf<TaskDraft>()

    private var dueAtMillis: Long? = null

    private var currentUid: String? = null
    private var editProjectId: String? = null
    private var isEditMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_edit_project)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            startActivity(Intent(this, Sign_in::class.java))
            finish()
            return
        }

        bindViews()
        setupLists()

        // read intent => edit mode?
        editProjectId = intent.getStringExtra("projectId")
        isEditMode = !editProjectId.isNullOrBlank()

        btnBack.setOnClickListener { finish() }
        tvDueDate.setOnClickListener { openDatePicker() }

        btnAddTask.setOnClickListener { tasksAdapter.addEmptyTask() }

        btnAddAssignee.setOnClickListener {
            Toast.makeText(this, "Assign/edit team later (edit_team)", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            val uid = currentUid ?: return@setOnClickListener
            if (isEditMode) updateProject(editProjectId!!, uid) else createProject(uid)
        }

        if (isEditMode) {
            loadProjectForEdit(editProjectId!!)
        } else {
            // default new project init
            assignees.clear()
            taskDrafts.clear()
            assignees.add(AssigneeDraft(uid = currentUid!!, photoBase64 = null))
            taskDrafts.add(TaskDraft())
            assigneesAdapter.notifyDataSetChanged()
            tasksAdapter.setTasks(taskDrafts)
        }
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSave)

        etTitle = findViewById(R.id.etProjectTitle)
        etDesc = findViewById(R.id.etProjectDesc)
        tvDueDate = findViewById(R.id.tvDueDate)

        rvAssignees = findViewById(R.id.rvAssignees)
        btnAddAssignee = findViewById(R.id.btnAddAssignee)

        rvTasks = findViewById(R.id.rvProjectTasks)
        btnAddTask = findViewById(R.id.btnAddTask)
    }

    private fun setupLists() {
        assigneesAdapter = AssigneesAdapter(assignees)
        rvAssignees.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvAssignees.adapter = assigneesAdapter

        tasksAdapter = ProjectTasksDraftAdapter(taskDrafts)
        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = tasksAdapter
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

    // ---------- CREATE ----------
    private fun createProject(creatorUid: String) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            startActivity(Intent(this, No_Internet_Connection::class.java))
            overridePendingTransition(0, 0)
            return
        }
        val name = etTitle.text.toString().trim()
        val desc = etDesc.text.toString().trim()

        if (name.isEmpty()) {
            etTitle.error = "Required"
            etTitle.requestFocus()
            Toast.makeText(this, "Project title is required", Toast.LENGTH_SHORT).show()
            return
        }

        val tasks = tasksAdapter.getTasks().filter { it.title.isNotBlank() }
        if (tasks.isEmpty()) {
            Toast.makeText(this, "Project must have at least 1 task", Toast.LENGTH_SHORT).show()
            return
        }

        val now = System.currentTimeMillis()
        val projectId = dbRef.child("projects").push().key ?: run {
            Toast.makeText(this, "Failed to generate project id", Toast.LENGTH_SHORT).show()
            return
        }

        val project = Project(
            id = projectId,
            name = name,
            description = desc,
            createdBy = creatorUid,
            status = "in_progress",
            createdAt = now,
            updatedAt = now,
            dueAt = dueAtMillis,
            tasksTotal = tasks.size,
            tasksDone = 0
        )

        val updates = hashMapOf<String, Any?>()
        updates["projects/$projectId"] = project
        updates["userProjects/$creatorUid/$projectId"] = true
        updates["projectMembers/$projectId/$creatorUid/role"] = "creator"

        for (t in tasks) {
            val taskId = dbRef.child("projectTasks").child(projectId).push().key ?: continue
            updates["projectTasks/$projectId/$taskId"] = mapOf(
                "id" to taskId,
                "projectId" to projectId,
                "title" to t.title,
                "hours" to t.hours,
                "status" to "in_progress",
                "createdBy" to creatorUid,
                "createdAt" to now,
                "updatedAt" to now
            )
        }

        dbRef.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Project created", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, home_page::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.localizedMessage ?: "Failed to save", Toast.LENGTH_LONG).show()
            }
    }

    // ---------- LOAD FOR EDIT ----------
    private fun loadProjectForEdit(projectId: String) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            startActivity(Intent(this, No_Internet_Connection::class.java))
            overridePendingTransition(0, 0)
            return
        }
        // load main project
        dbRef.child("projects").child(projectId).get()
            .addOnSuccessListener { snap ->
                val p = snap.getValue(Project::class.java)
                if (p == null) {
                    Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                etTitle.setText(p.name ?: "")
                etDesc.setText(p.description ?: "")
                dueAtMillis = p.dueAt

                if (dueAtMillis != null) {
                    val c = Calendar.getInstance()
                    c.timeInMillis = dueAtMillis!!
                    tvDueDate.text = "Due date: ${c.get(Calendar.DAY_OF_MONTH)}/${c.get(Calendar.MONTH) + 1}/${c.get(Calendar.YEAR)}"
                } else {
                    tvDueDate.text = "Due date: Not set"
                }

                loadTasksForEdit(projectId)
                loadMembersForEdit(projectId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.localizedMessage ?: "Failed to load", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadTasksForEdit(projectId: String) {
        dbRef.child("projectTasks").child(projectId).get()
            .addOnSuccessListener { snap ->
                val list = mutableListOf<TaskDraft>()
                for (tSnap in snap.children) {
                    val id = tSnap.key
                    val title = tSnap.child("title").getValue(String::class.java) ?: ""
                    val hoursAny = tSnap.child("hours").value
                    val hours = when (hoursAny) {
                        is Long -> hoursAny.toInt()
                        is Int -> hoursAny
                        else -> 0
                    }

                    val status = tSnap.child("status").getValue(String::class.java) ?: "in_progress"
                    if (id != null) list.add(TaskDraft(id = id, title = title, hours = hours, status = status))
                }
                if (list.isEmpty()) list.add(TaskDraft())
                taskDrafts.clear()
                taskDrafts.addAll(list)
                tasksAdapter.setTasks(taskDrafts)
            }
    }

    private fun loadMembersForEdit(projectId: String) {
        dbRef.child("projectMembers").child(projectId).get()
            .addOnSuccessListener { snap ->
                val list = mutableListOf<AssigneeDraft>()
                for (m in snap.children) {
                    val uid = m.key ?: continue
                    list.add(AssigneeDraft(uid = uid, photoBase64 = null))
                }
                if (list.isEmpty() && currentUid != null) {
                    list.add(AssigneeDraft(uid = currentUid!!, photoBase64 = null))
                }
                assignees.clear()
                assignees.addAll(list)
                assigneesAdapter.notifyDataSetChanged()
            }
    }

    // ---------- UPDATE ----------
    private fun updateProject(projectId: String, editorUid: String) {
        if (!NetworkUtils.isInternetAvailable(this)) {
            startActivity(Intent(this, No_Internet_Connection::class.java))
            overridePendingTransition(0, 0)
            return
        }
        val name = etTitle.text.toString().trim()
        val desc = etDesc.text.toString().trim()

        if (name.isEmpty()) {
            etTitle.error = "Required"
            etTitle.requestFocus()
            Toast.makeText(this, "Project title is required", Toast.LENGTH_SHORT).show()
            return
        }

        val drafts = tasksAdapter.getTasks()
        val keep = drafts.filter { it.title.isNotBlank() }
        if (keep.isEmpty()) {
            Toast.makeText(this, "Project must have at least 1 task", Toast.LENGTH_SHORT).show()
            return
        }

        val now = System.currentTimeMillis()

        // We do a proper update without creating a new projectId
        dbRef.child("projectTasks").child(projectId).get()
            .addOnSuccessListener { existingSnap ->

                val existingIds = existingSnap.children.mapNotNull { it.key }.toSet()
                val presentIds = mutableSetOf<String>()

                val updates = hashMapOf<String, Any?>()

                // update main project fields (don’t change createdBy/createdAt)
                updates["projects/$projectId/name"] = name
                updates["projects/$projectId/description"] = desc
                updates["projects/$projectId/dueAt"] = dueAtMillis
                updates["projects/$projectId/updatedAt"] = now

                // tasks: update existing / create new / remove deleted
                for (t in drafts) {
                    val title = t.title.trim()
                    if (title.isBlank()) {
                        // if it was an existing task and user cleared it => delete
                        if (!t.id.isNullOrBlank()) {
                            updates["projectTasks/$projectId/${t.id}"] = null
                        }
                        continue
                    }

                    val taskId = if (t.id.isNullOrBlank()) {
                        dbRef.child("projectTasks").child(projectId).push().key ?: continue
                    } else t.id!!

                    t.id = taskId
                    presentIds.add(taskId)

                    val status = t.status.ifBlank { "in_progress" }

                    updates["projectTasks/$projectId/$taskId"] = mapOf(
                        "id" to taskId,
                        "projectId" to projectId,
                        "title" to title,
                        "hours" to t.hours,
                        "status" to status,
                        "updatedAt" to now,
                        "editedBy" to editorUid
                    )
                }

                // remove tasks that existed but are no longer present
                for (oldId in existingIds) {
                    if (!presentIds.contains(oldId)) {
                        updates["projectTasks/$projectId/$oldId"] = null
                    }
                }

                // update totals based on current drafts
                val total = presentIds.size
                val done = drafts.count { it.title.isNotBlank() && (it.status == "done" || it.status == "completed") }

                updates["projects/$projectId/tasksTotal"] = total
                updates["projects/$projectId/tasksDone"] = done

                dbRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Project updated", Toast.LENGTH_SHORT).show()
                        finish() // go back to project_detail/home
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, e.localizedMessage ?: "Update failed", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.localizedMessage ?: "Failed to load tasks", Toast.LENGTH_SHORT).show()
            }
    }
}
