package com.schoolms.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StudentDao {
    @Query("SELECT * FROM students")
    suspend fun getAllStudents(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(students: List<StudentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Query("DELETE FROM students")
    suspend fun clearAll()
}

@Dao
interface AttendanceRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<AttendanceRecordEntity>)

    @Query("SELECT * FROM offline_queue_attendance WHERE isSynced = 0")
    suspend fun getUnsyncedRecords(): List<AttendanceRecordEntity>

    @Query("UPDATE offline_queue_attendance SET isSynced = 1 WHERE localId IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)
    
    @Query("DELETE FROM offline_queue_attendance WHERE isSynced = 1")
    suspend fun clearSyncedRecords()
}
