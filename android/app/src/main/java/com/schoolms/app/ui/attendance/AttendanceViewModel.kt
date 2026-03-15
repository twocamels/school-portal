package com.schoolms.app.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolms.app.data.local.AttendanceRecordDao
import com.schoolms.app.data.local.AttendanceRecordEntity
import com.schoolms.app.domain.model.Student
import com.schoolms.app.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * State representing the UI for the AttendanceRollScreen.
 */
data class AttendanceUiState(
    val isLoading: Boolean = false,
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val classId: String? = null,
    val studentsList: List<StudentAttendanceItem> = emptyList(),
    val error: String? = null,
    val isSuccess: Boolean = false // Set true when sync or offline save finishes
)

/**
 * Combines student details with their current marked attendance status (Present, Absent, Late).
 */
data class StudentAttendanceItem(
    val student: Student,
    var status: String = "present" // Default
)

/**
 * ViewModel for Teacher roll call.
 * 
 * Crucial feature: It attempts to sync to backend immediately. 
 * If offline, it saves to Room DB (offline_queue logic handled via WorkManager separately).
 */
@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val attendanceRecordDao: AttendanceRecordDao // Room abstract
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    /**
     * Initializes the class list. It loads from Room instantly (since StudentRepository 
     * caches students).
     */
    fun loadClass(classId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, classId = classId) }

            // Assume student repository can fetch students by class ID.
            // Simplified here: We fetch all students and filter locally for brevity.
            studentRepository.getStudents().collect { result ->
                result.onSuccess { students ->
                    val classRoster = students.filter { it.classId == classId }
                    val rosterItems = classRoster.map { StudentAttendanceItem(student = it) }
                    
                    _uiState.update { 
                        it.copy(isLoading = false, studentsList = rosterItems)
                    }
                }.onFailure { e ->
                    _uiState.update { 
                        it.copy(isLoading = false, error = "Failed to load class roster.")
                    }
                }
            }
        }
    }

    /**
     * Updates an individual student's status on the UI grid.
     */
    fun updateStudentStatus(studentId: String, newStatus: String) {
        _uiState.update { currentState ->
            val updatedList = currentState.studentsList.map { item ->
                if (item.student.id == studentId) item.copy(status = newStatus) else item
            }
            currentState.copy(studentsList = updatedList)
        }
    }

    /**
     * Attempts to save all marked records.
     * This relies heavily on Room for the "Offline Strategy".
     */
    fun submitAttendance() {
        val currentState = _uiState.value
        if (currentState.classId == null || currentState.studentsList.isEmpty()) {
            return
        }

        viewModelScope.launch {
            // 1. Prepare entities for Room
            val entities = currentState.studentsList.map { item ->
                AttendanceRecordEntity(
                    studentId = item.student.id,
                    classId = currentState.classId,
                    date = currentState.date,
                    status = item.status,
                    isSynced = false // Important: Marked false so WorkManager picks it up
                )
            }

            // 2. Save instantaneously to Room for fast UI response
            attendanceRecordDao.insertAll(entities)

            // 3. Mark success UI
            _uiState.update { it.copy(isSuccess = true, error = null) }
            
            // 4. Trigger SyncWorker explicitly here (Implementation omitted for brevity)
            // workManager.enqueueUniqueWork("SyncAttendance", ExistingWorkPolicy.REPLACE, syncRequest)
        }
    }

    fun consumeError() { _uiState.update { it.copy(error = null) } }
}
