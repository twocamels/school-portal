package com.schoolms.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.schoolms.app.domain.model.Student

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val firstName: String,
    val lastName: String,
    val admissionNumber: String,
    val classId: String?,
    val className: String?
) {
    fun toDomainModel() = Student(
        id = id,
        firstName = firstName,
        lastName = lastName,
        admissionNumber = admissionNumber,
        classId = classId,
        className = className
    )
}

// Extension to map from Domain -> Entity
fun Student.toEntity() = StudentEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    admissionNumber = admissionNumber,
    classId = classId,
    className = className
)

@Entity(tableName = "offline_queue_attendance")
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val studentId: String,
    val classId: String,
    val date: String,
    val status: String,
    val isSynced: Boolean = false
)
