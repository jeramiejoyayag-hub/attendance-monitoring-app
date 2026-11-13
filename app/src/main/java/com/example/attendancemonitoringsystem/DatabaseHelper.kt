package com.example.attendancemonitoringsystem

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "AttendanceMonitoringSystem.db"
        private const val DATABASE_VERSION = 3

        const val TABLE_USERS = "users"
        const val COLUMN_USER_ID = "id"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD = "password"

        const val TABLE_ATTENDANCE = "attendance_records"
        const val COLUMN_ATT_ID = "att_id"
        const val COLUMN_STUDENT_NAME = "student_name"
        const val COLUMN_COURSE = "course"
        const val COLUMN_SUBJECT = "subject"
        const val COLUMN_STATUS = "status"
        const val COLUMN_DETAILS = "details"
        const val COLUMN_DATE = "date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = ("CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_EMAIL + " TEXT UNIQUE,"
                + COLUMN_PASSWORD + " TEXT)")

        val createAttendanceTable = ("CREATE TABLE " + TABLE_ATTENDANCE + "("
                + COLUMN_ATT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_STUDENT_NAME + " TEXT,"
                + COLUMN_COURSE + " TEXT,"
                + COLUMN_SUBJECT + " TEXT,"
                + COLUMN_STATUS + " TEXT,"
                + COLUMN_DETAILS + " TEXT,"
                + COLUMN_DATE + " TEXT)")

        db.execSQL(createUsersTable)
        db.execSQL(createAttendanceTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ATTENDANCE")
        onCreate(db)
    }

    fun addUser(email: String, pass: String): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(COLUMN_EMAIL, email)
        cv.put(COLUMN_PASSWORD, pass)
        val result = db.insert(TABLE_USERS, null, cv)
        return result != -1L
    }

    fun checkUser(email: String, pass: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL=? AND $COLUMN_PASSWORD=?", arrayOf(email, pass))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun addAttendance(name: String, course: String, subject: String, status: String, details: String, date: String): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(COLUMN_STUDENT_NAME, name)
        cv.put(COLUMN_COURSE, course)
        cv.put(COLUMN_SUBJECT, subject)
        cv.put(COLUMN_STATUS, status)
        cv.put(COLUMN_DETAILS, details)
        cv.put(COLUMN_DATE, date)
        val result = db.insert(TABLE_ATTENDANCE, null, cv)
        return result != -1L
    }

    // NEW: delete one record by its ID
    fun deleteAttendance(id: Int): Boolean {
        val db = this.writableDatabase
        val rows = db.delete(TABLE_ATTENDANCE, "$COLUMN_ATT_ID=?", arrayOf(id.toString()))
        return rows > 0
    }

    // NEW: update name, course, subject, status and notes of one record (date stays the same)
    fun updateAttendance(id: Int, name: String, course: String, subject: String, status: String, details: String): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(COLUMN_STUDENT_NAME, name)
        cv.put(COLUMN_COURSE, course)
        cv.put(COLUMN_SUBJECT, subject)
        cv.put(COLUMN_STATUS, status)
        cv.put(COLUMN_DETAILS, details)
        val rows = db.update(TABLE_ATTENDANCE, cv, "$COLUMN_ATT_ID=?", arrayOf(id.toString()))
        return rows > 0
    }

    fun getCountsForDate(date: String? = null): Map<String, Int> {
        val counts = mutableMapOf("Present" to 0, "Absent" to 0, "Late" to 0, "Total" to 0)
        val db = this.readableDatabase
        val query = if (date.isNullOrEmpty()) {
            "SELECT status, COUNT(*) as cnt FROM $TABLE_ATTENDANCE GROUP BY status"
        } else {
            "SELECT status, COUNT(*) as cnt FROM $TABLE_ATTENDANCE WHERE $COLUMN_DATE=? GROUP BY status"
        }
        val args = if (date.isNullOrEmpty()) null else arrayOf(date)
        val cursor = db.rawQuery(query, args)
        var total = 0
        if (cursor.moveToFirst()) {
            do {
                val status = cursor.getString(0) ?: ""
                val count = cursor.getInt(1)
                total += count
                when {
                    status.contains("Present", ignoreCase = true) -> counts["Present"] = (counts["Present"] ?: 0) + count
                    status.contains("Absent", ignoreCase = true) -> counts["Absent"] = (counts["Absent"] ?: 0) + count
                    status.contains("Late", ignoreCase = true) -> counts["Late"] = (counts["Late"] ?: 0) + count
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        counts["Total"] = total
        return counts
    }

    fun getAllAttendance(): List<String> {
        val list = mutableListOf<String>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_ATTENDANCE ORDER BY $COLUMN_ATT_ID DESC", null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ATT_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STUDENT_NAME))
                val courseIdx = cursor.getColumnIndex(COLUMN_COURSE)
                val course = if (courseIdx != -1) cursor.getString(courseIdx) ?: "" else ""
                val subjectIdx = cursor.getColumnIndex(COLUMN_SUBJECT)
                val subject = if (subjectIdx != -1) cursor.getString(subjectIdx) ?: "" else ""
                val status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS))
                val details = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DETAILS))
                val dateIdx = cursor.getColumnIndex(COLUMN_DATE)
                val date = if (dateIdx != -1) cursor.getString(dateIdx) ?: "" else ""

                val sb = StringBuilder()
                sb.append("ID: ").append(id).append("\n")
                sb.append("Student: ").append(name)
                if (date.isNotEmpty()) sb.append("\nDate: ").append(date)
                if (course.isNotEmpty()) sb.append("\nCourse: ").append(course)
                if (subject.isNotEmpty()) sb.append("\nSubject: ").append(subject)
                sb.append("\nStatus: ").append(status)
                if (details.isNotEmpty()) sb.append("\nDetails: ").append(details)

                list.add(sb.toString())
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}