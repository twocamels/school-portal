package com.schoolms.app.domain.repository

import com.schoolms.app.data.local.StudentDao
import com.schoolms.app.data.local.StudentEntity
import com.schoolms.app.data.remote.ApiService
import com.schoolms.app.domain.model.Student
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StudentRepository abstracts the data source for the rest of the app.
 * It coordinates fetching fresh data from the network and caching it
 * into Room for offline reads.
 */
@Singleton
class StudentRepository @Inject constructor(
    private val apiService: ApiService,
    private val studentDao: StudentDao // Abstracted Room access
) {
    /**
     * Fetch the list of students.
     * 1. Emit what we have in the local database immediately.
     * 2. Hit the network to fetch the latest.
     * 3. Sync to local DB.
     * 4. Emit the fresh data.
     */
    fun getStudents(page: Int = 1, perPage: Int = 30): Flow<Result<List<Student>>> = flow {
        // Step 1: Read Cache (TTL strategy/offline fallback)
        val localData = studentDao.getAllStudents()
        if (localData.isNotEmpty()) {
            emit(Result.success(localData.map { it.toDomainModel() }))
        }

        // Step 2: Network Fetch
        try {
            val response = apiService.getStudents(page, perPage)
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                val networkStudents = apiResponse.data ?: emptyList()
                
                // Step 3: Cache to Room
                // We map back to our Room entity models
                studentDao.insertAll(networkStudents.map { it.toEntity() })

                // Step 4: Emit fresh data
                emit(Result.success(networkStudents))
            } else {
                // If the network request failed (e.g. 500 error), and we had no local data, emit error.
                if (localData.isEmpty()) {
                    emit(Result.failure(Exception("Failed to fetch students from server: ${response.code()}")))
                }
            }
        } catch (e: Exception) {
            // Network completely down / timeout
            // If we emitted local data already, the UI just shows stale data gracefully.
            // If not, we must emit the error.
            if (localData.isEmpty()) {
                emit(Result.failure(e))
            }
        }
    }

    /**
     * Fetch a single student's details, prioritizing network for freshness.
     */
    suspend fun getStudentDetails(id: String): Result<Student> {
        return try {
            val response = apiService.getStudentById(id)
            if (response.isSuccessful && response.body() != null) {
                val student = response.body()!!.data!!
                // Update specific record in Room
                studentDao.insertStudent(student.toEntity())
                Result.success(student)
            } else {
                // Fallback to offline
                val localData = studentDao.getStudentById(id)
                if (localData != null) {
                    Result.success(localData.toDomainModel())
                } else {
                    Result.failure(Exception("Student not found locally or remotely."))
                }
            }
        } catch (e: Exception) {
            // Fallback to offline
            val localData = studentDao.getStudentById(id)
            if (localData != null) {
                Result.success(localData.toDomainModel())
            } else {
                Result.failure(e)
            }
        }
    }
}
