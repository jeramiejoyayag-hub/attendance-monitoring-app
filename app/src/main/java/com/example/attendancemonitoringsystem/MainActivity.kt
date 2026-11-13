package com.example.attendancemonitoringsystem

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var selectedStatus: String? = null
    private val selectedCalendar = Calendar.getInstance()

    private lateinit var tvDateNavigator: TextView
    private lateinit var tvPresentCount: TextView
    private lateinit var tvAbsentCount: TextView
    private lateinit var tvLateCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        val etStudentName = findViewById<EditText>(R.id.etStudentName)
        val etStudentCourse = findViewById<EditText>(R.id.etStudentCourse)
        val etSubject = findViewById<EditText>(R.id.etSubject)

        val rbPresent = findViewById<RadioButton>(R.id.rbPresent)
        val rbLate = findViewById<RadioButton>(R.id.rbLate)
        val rbAbsent = findViewById<RadioButton>(R.id.rbAbsent)
        val rbExcused = findViewById<RadioButton>(R.id.rbExcused)

        val cbUniform = findViewById<CheckBox>(R.id.cbUniform)
        val cbHomework = findViewById<CheckBox>(R.id.cbHomework)
        val cbMedical = findViewById<CheckBox>(R.id.cbMedicalNote)
        val cbParent = findViewById<CheckBox>(R.id.cbParentNotified)

        val btnSave = findViewById<Button>(R.id.btnSaveAttendance)
        val btnLogs = findViewById<Button>(R.id.btnViewLogs)

        val llDateContainer = findViewById<LinearLayout>(R.id.llDateContainer)
        tvDateNavigator = findViewById(R.id.tvDateNavigator)

        tvPresentCount = findViewById(R.id.tvPresentCount)
        tvAbsentCount = findViewById(R.id.tvAbsentCount)
        tvLateCount = findViewById(R.id.tvLateCount)

        val radioButtons = listOf(rbPresent, rbLate, rbAbsent, rbExcused)
        radioButtons.forEach { radioButton ->
            radioButton.setOnClickListener {
                radioButtons.forEach { rb -> rb.isChecked = (rb == radioButton) }
                selectedStatus = radioButton.text.toString()
            }
        }

        updateDateDisplay()
        updateCountsDisplay()

        val datePickerListener = {
            val year = selectedCalendar[Calendar.YEAR]
            val month = selectedCalendar[Calendar.MONTH]
            val day = selectedCalendar[Calendar.DAY_OF_MONTH]

            DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    selectedCalendar[Calendar.YEAR] = selectedYear
                    selectedCalendar[Calendar.MONTH] = selectedMonth
                    selectedCalendar[Calendar.DAY_OF_MONTH] = selectedDay
                    updateDateDisplay()
                    updateCountsDisplay()
                },
                year,
                month,
                day
            ).show()
        }

        llDateContainer.setOnClickListener { datePickerListener() }
        tvDateNavigator.setOnClickListener { datePickerListener() }

        btnSave.setOnClickListener {
            val name = etStudentName.text.toString().trim()
            val course = etStudentCourse.text.toString().trim()
            val subject = etSubject.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter student name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (course.isEmpty()) {
                Toast.makeText(this, "Please enter course or program", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (subject.isEmpty()) {
                Toast.makeText(this, "Please enter subject", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedStatus.isNullOrEmpty()) {
                Toast.makeText(this, "Please select an attendance status", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val detailsList = mutableListOf<String>()
            if (cbUniform.isChecked) detailsList.add(cbUniform.text.toString())
            if (cbHomework.isChecked) detailsList.add(cbHomework.text.toString())
            if (cbMedical.isChecked) detailsList.add(cbMedical.text.toString())
            if (cbParent.isChecked) detailsList.add(cbParent.text.toString())

            val detailsText = if (detailsList.isEmpty()) "None" else detailsList.joinToString(", ")
            val formattedDate = getDbFormattedDate()

            val saved = dbHelper.addAttendance(name, course, subject, selectedStatus!!, detailsText, formattedDate)
            if (saved) {
                Toast.makeText(this, "Attendance Recorded Successfully!", Toast.LENGTH_SHORT).show()
                etStudentName.text.clear()
                etStudentCourse.text.clear()
                etSubject.text.clear()
                radioButtons.forEach { rb -> rb.isChecked = false }
                selectedStatus = null
                cbUniform.isChecked = false
                cbHomework.isChecked = false
                cbMedical.isChecked = false
                cbParent.isChecked = false

                updateCountsDisplay()
            } else {
                Toast.makeText(this, "Failed to record attendance", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogs.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun updateDateDisplay() {
        val displayFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        val formatted = displayFormat.format(selectedCalendar.time)
        tvDateNavigator.text = getString(R.string.app_name)
        tvDateNavigator.text = "‹   📅 $formatted   ›"
    }

    private fun getDbFormattedDate(): String {
        val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dbFormat.format(selectedCalendar.time)
    }

    private fun updateCountsDisplay() {
        val dbDate = getDbFormattedDate()
        val counts = dbHelper.getCountsForDate(dbDate)

        val present = counts["Present"] ?: 0
        val absent = counts["Absent"] ?: 0
        val late = counts["Late"] ?: 0

        tvPresentCount.text = present.toString()
        tvAbsentCount.text = absent.toString()
        tvLateCount.text = late.toString()
    }

    override fun onResume() {
        super.onResume()
        updateCountsDisplay()
    }
}