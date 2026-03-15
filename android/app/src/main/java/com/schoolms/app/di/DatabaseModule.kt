package com.schoolms.app.di

import android.content.Context
import androidx.room.Room
import com.schoolms.app.data.local.AppDatabase
import com.schoolms.app.data.local.AttendanceRecordDao
import com.schoolms.app.data.local.StudentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "schoolms_offline.db"
        )
        // Normally we'd handle migrations here
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideStudentDao(database: AppDatabase): StudentDao {
        return database.studentDao()
    }

    @Provides
    fun provideAttendanceRecordDao(database: AppDatabase): AttendanceRecordDao {
        return database.attendanceRecordDao()
    }
}
