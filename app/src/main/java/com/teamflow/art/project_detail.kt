package com.teamflow.art

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class project_detail : AppCompatActivity() {

    private val dbRef = FirebaseDatabase.getInstance().reference

    private lateinit var btnBack: ImageView
    private lateinit var btnEdit: ImageView

    private lateinit var tvTitle: TextView
    private lateinit var tvDesc: TextView
    private lateinit var tvDue: TextView

    private lateinit var rvMembers: RecyclerView
    private lateinit var rvTasks: RecyclerView

    private lateinit var membersAdapter: ProjectMembersAdapter
    private lateinit var tasksAdapter: ProjectDetailTasksAdapter

    private val memberItems = mutableListOf<MemberItem>()
    private val taskItems = mutableListOf<ProjectTaskItem>()

    private var projectId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_project_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        projectId = intent.getStringExtra("projectId")
        if (projectId.isNullOrBlank()) {
            Toast.makeText(this, "Missing project id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        setupLists()

        btnBack.setOnClickListener { finish() }

        btnEdit.setOnClickListener {
            val i = Intent(this, add_edit_project::class.java)
            i.putExtra("projectId", projectId)
            startActivity(i)
        }

        refreshAll()
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        btnEdit = findViewById(R.id.btnEdit)

        tvTitle = findViewById(R.id.projectTitle)
        tvDesc = findViewById(R.id.projectDesc)
        tvDue = findViewById(R.id.dueDate)

        rvMembers = findViewById(R.id.rvMembers)
        rvTasks = findViewById(R.id.rvProjectTasks)
    }

    private fun setupLists() {
        membersAdapter = ProjectMembersAdapter(memberItems)
        rvMembers.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvMembers.adapter = membersAdapter

        tasksAdapter = ProjectDetailTasksAdapter(taskItems)
        rvTasks.layoutManager = LinearLayoutManager(this)
        rvTasks.adapter = tasksAdapter
    }

    private fun refreshAll() {
        val pid = projectId ?: return
        loadProject(pid)
        loadTasks(pid)
        loadMembers(pid)
    }

    private fun loadProject(pid: String) {
        dbRef.child("projects").child(pid).get()
            .addOnSuccessListener { snap ->
                val p = snap.getValue(Project::class.java)
                if (p == null) {
                    Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                tvTitle.text = p.name ?: "Untitled project"
                tvDesc.text = p.description ?: ""

                val due = p.dueAt
                if (due != null) {
                    val fmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
                    tvDue.text = "Due date: ${fmt.format(Date(due))}"
                } else {
                    tvDue.text = "Due date: Not set"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.localizedMessage ?: "Failed to load project", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTasks(pid: String) {
        dbRef.child("projectTasks").child(pid).get()
            .addOnSuccessListener { snap ->
                val list = mutableListOf<ProjectTaskItem>()
                for (tSnap in snap.children) {
                    val id = tSnap.key ?: continue
                    val title = tSnap.child("title").getValue(String::class.java) ?: ""
                    val status = tSnap.child("status").getValue(String::class.java) ?: "in_progress"

                    val hoursAny = tSnap.child("hours").value
                    val hours = when (hoursAny) {
                        is Long -> hoursAny.toInt()
                        is Int -> hoursAny
                        is String -> hoursAny.toIntOrNull() ?: 0
                        else -> 0
                    }

                    if (title.isNotBlank()) {
                        list.add(ProjectTaskItem(id = id, title = title, hours = hours, status = status))
                    }
                }
                tasksAdapter.setTasks(list)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.localizedMessage ?: "Failed to load tasks", Toast.LENGTH_SHORT).show()
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

                // Try to enrich from /users/{uid} if you have it; else it still works with uid only
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
                            temp.add(MemberItem(uid = uid, name = "", photoBase64 = null))
                        }
                        .addOnCompleteListener {
                            remaining--
                            if (remaining == 0) {
                                // keep stable order (same as uids)
                                val ordered = uids.map { id -> temp.firstOrNull { it.uid == id } ?: MemberItem(uid = id) }
                                membersAdapter.setMembers(ordered)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.localizedMessage ?: "Failed to load members", Toast.LENGTH_SHORT).show()
            }
    }
}
