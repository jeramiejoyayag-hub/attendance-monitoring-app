package com.example.attendancemonitoringsystem

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var container: LinearLayout

    private val statusOptions = listOf("Present", "Late", "Absent", "Excused")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        dbHelper = DatabaseHelper(this)
        container = findViewById(R.id.llLogsContainer)
        val btnBack = findViewById<Button>(R.id.btnBackToMain)

        btnBack.setOnClickListener {
            finish()
        }

        loadLogs()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    // Builds the whole list. Called again after every edit or delete.
    private fun loadLogs() {
        container.removeAllViews()

        val logs = dbHelper.getAllAttendance()

        if (logs.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No attendance records found."
                setTextColor(Color.parseColor("#606A62"))
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(24, 48, 24, 48)
            }
            container.addView(emptyTv)
            return
        }

        val hintTv = TextView(this).apply {
            text = "Tap a record to edit • Hold to delete"
            setTextColor(Color.parseColor("#8E9690"))
            textSize = 11f
            setPadding(0, 0, 0, 16)
        }
        container.addView(hintTv)

        for (log in logs) {
            val cardLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(32, 24, 32, 24)
                setBackgroundResource(R.drawable.bg_card_cream)
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
            }

            val lines = log.split("\n")
            val id = lines.find { it.startsWith("ID:") }?.removePrefix("ID:")?.trim()?.toIntOrNull() ?: -1
            val studentName = lines.find { it.startsWith("Student:") }?.removePrefix("Student:")?.trim() ?: log
            val date = lines.find { it.startsWith("Date:") }?.removePrefix("Date:")?.trim() ?: ""
            val course = lines.find { it.startsWith("Course:") }?.removePrefix("Course:")?.trim() ?: ""
            val subject = lines.find { it.startsWith("Subject:") }?.removePrefix("Subject:")?.trim() ?: ""
            val status = lines.find { it.startsWith("Status:") }?.removePrefix("Status:")?.trim() ?: "Recorded"
            val details = lines.find { it.startsWith("Details:") }?.removePrefix("Details:")?.trim() ?: ""

            val courseSubjectInfo = buildString {
                if (course.isNotEmpty()) append(course)
                if (subject.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append(subject)
                }
            }

            val initials = if (studentName.contains(" ")) {
                val parts = studentName.split(" ")
                if (parts.size >= 2 && parts[1].isNotEmpty()) {
                    "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                } else {
                    studentName.take(2).uppercase()
                }
            } else {
                studentName.take(2).uppercase()
            }

            val avatarTv = TextView(this).apply {
                text = initials
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#1E3A2B"))
                setBackgroundResource(R.drawable.bg_avatar_circle)
                layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                    marginEnd = 24
                }
            }

            val textColumn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val nameTv = TextView(this).apply {
                text = studentName
                setTextColor(Color.parseColor("#1C251F"))
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
            }
            textColumn.addView(nameTv)

            if (date.isNotEmpty()) {
                val dateTv = TextView(this).apply {
                    text = "📅 $date"
                    setTextColor(Color.parseColor("#8E9690"))
                    textSize = 11f
                }
                textColumn.addView(dateTv)
            }

            if (courseSubjectInfo.isNotEmpty()) {
                val csTv = TextView(this).apply {
                    text = courseSubjectInfo
                    setTextColor(Color.parseColor("#4E7B5C"))
                    textSize = 11f
                    setTypeface(null, Typeface.BOLD)
                }
                textColumn.addView(csTv)
            }

            val detailsTv = TextView(this).apply {
                text = if (details.isNotEmpty() && details != "None") "Notes: $details" else "Roll call verified"
                setTextColor(Color.parseColor("#606A62"))
                textSize = 11f
            }
            textColumn.addView(detailsTv)

            val statusTv = TextView(this).apply {
                text = "• $status"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setPadding(20, 8, 20, 8)

                when {
                    status.contains("Present", ignoreCase = true) -> {
                        setBackgroundResource(R.drawable.bg_pill_green)
                        setTextColor(Color.parseColor("#1E6B34"))
                    }
                    status.contains("Late", ignoreCase = true) -> {
                        setBackgroundResource(R.drawable.bg_pill_orange)
                        setTextColor(Color.parseColor("#B86E00"))
                    }
                    status.contains("Absent", ignoreCase = true) -> {
                        setBackgroundResource(R.drawable.bg_pill_red)
                        setTextColor(Color.parseColor("#C0392B"))
                    }
                    else -> {
                        setBackgroundResource(R.drawable.bg_pill_cream)
                        setTextColor(Color.parseColor("#5A5243"))
                    }
                }
            }

            cardLayout.addView(avatarTv)
            cardLayout.addView(textColumn)
            cardLayout.addView(statusTv)

            // Tap = edit, long press = delete
            if (id != -1) {
                cardLayout.setOnClickListener {
                    showEditDialog(id, studentName, course, subject, status, details)
                }
                cardLayout.setOnLongClickListener {
                    showDeleteDialog(id, studentName)
                    true
                }
            }

            container.addView(cardLayout)
        }
    }

    private fun showDeleteDialog(id: Int, studentName: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete record?")
            .setMessage("This will permanently delete the attendance record for $studentName.")
            .setPositiveButton("Delete") { _, _ ->
                if (dbHelper.deleteAttendance(id)) {
                    Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show()
                    loadLogs()
                } else {
                    Toast.makeText(this, "Could not delete record", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(id: Int, name: String, course: String, subject: String, status: String, details: String) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(0))
        }

        val etName = EditText(this).apply {
            hint = "Student name"
            setText(name)
        }
        val etCourse = EditText(this).apply {
            hint = "Course / Program"
            setText(course)
        }
        val etSubject = EditText(this).apply {
            hint = "Subject"
            setText(subject)
        }
        val etNotes = EditText(this).apply {
            hint = "Notes"
            setText(if (details == "None") "" else details)
        }
        val spinnerStatus = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@HistoryActivity,
                android.R.layout.simple_spinner_dropdown_item,
                statusOptions
            )
            val index = statusOptions.indexOfFirst { status.contains(it, ignoreCase = true) }
            if (index != -1) setSelection(index)
        }

        form.addView(etName)
        form.addView(etCourse)
        form.addView(etSubject)
        form.addView(etNotes)
        form.addView(spinnerStatus)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit record")
            .setView(form)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        // Set the Save click here so the dialog stays open when a field is empty
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newName = etName.text.toString().trim()
                val newCourse = etCourse.text.toString().trim()
                val newSubject = etSubject.text.toString().trim()
                val newStatus = spinnerStatus.selectedItem.toString()
                val newNotes = etNotes.text.toString().trim().ifEmpty { "None" }

                if (newName.isEmpty()) {
                    etName.error = "Please enter the student's name"
                    return@setOnClickListener
                }
                if (newCourse.isEmpty()) {
                    etCourse.error = "Please enter the course or program"
                    return@setOnClickListener
                }
                if (newSubject.isEmpty()) {
                    etSubject.error = "Please enter the subject"
                    return@setOnClickListener
                }

                if (dbHelper.updateAttendance(id, newName, newCourse, newSubject, newStatus, newNotes)) {
                    Toast.makeText(this, "Record updated", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadLogs()
                } else {
                    Toast.makeText(this, "Could not update record", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }
}