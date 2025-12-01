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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        val uid = auth.currentUser?.uid
        if (uid == null) {
            startActivity(Intent(this, Sign_in::class.java))
            finish()
            return
        }

        bindViews()

        // --- default: creator assigned + 1 empty task
        assignees.add(AssigneeDraft(uid = uid, photoBase64 = null))
        taskDrafts.add(TaskDraft())

        // Assignees Recycler
        assigneesAdapter = AssigneesAdapter(assignees)
        rvAssignees.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvAssignees.adapter = assigneesAdapter

        // Tasks Recycler
        tasksAdapter = ProjectTasksDraftAdapter(taskDrafts)
        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = tasksAdapter

        btnBack.setOnClickListener { finish() }

        tvDueDate.setOnClickListener { openDatePicker() }

        btnAddTask.setOnClickListener {
            tasksAdapter.addEmptyTask()
        }

        btnAddAssignee.setOnClickListener {
            // Later: open edit_team
            Toast.makeText(this, "Assign/edit team later (edit_team)", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            saveProject(uid)
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

    private fun saveProject(creatorUid: String) {
        val name = etTitle.text.toString().trim()
        val desc = etDesc.text.toString().trim()

        if (name.isEmpty()) {
            etTitle.error = "Required"
            etTitle.requestFocus()
            Toast.makeText(this, "Project title is required", Toast.LENGTH_SHORT).show()
            return
        }

        val tasks = tasksAdapter.getTasks()
        if (tasks.isEmpty()) {
            Toast.makeText(this, "Project must have at least 1 task", Toast.LENGTH_SHORT).show()
            return
        }

        // At least 1 task with a title
        if (tasks[0].title.isBlank()) {
            Toast.makeText(this, "First task title is required", Toast.LENGTH_SHORT).show()
            return
        }

        val now = System.currentTimeMillis()
        val projectId = dbRef.child("projects").push().key
        if (projectId == null) {
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

        // Project main
        updates["projects/$projectId"] = project

        // Creator assignment index
        updates["userProjects/$creatorUid/$projectId"] = true
        updates["projectMembers/$projectId/$creatorUid/role"] = "creator"

        // Tasks (minimum: supports your rule "each project must have one task")
        for (t in tasks) {
            if (t.title.isBlank()) continue // allow empty extra drafts
            val taskId = dbRef.child("projectTasks").child(projectId).push().key ?: continue

            val taskObj = mapOf(
                "id" to taskId,
                "projectId" to projectId,
                "title" to t.title,
                "hours" to t.hours,
                "status" to "in_progress",
                "createdBy" to creatorUid,
                "createdAt" to now,
                "updatedAt" to now
            )

            updates["projectTasks/$projectId/$taskId"] = taskObj
        }

        // If user added extra blank tasks and ONLY first task exists, still okay.
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
}
