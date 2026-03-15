package com.schoolms.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [StudentEntity::class, AttendanceRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao
}
