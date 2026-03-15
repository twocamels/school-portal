package com.schoolms.app.ui.scores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolms.app.domain.model.Student
import com.schoolms.app.domain.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State representing the UI for the ScoreEntryScreen.
 */
data class ScoreUiState(
    val isLoading: Boolean = false,
    val classId: String? = null,
    val subjectId: String? = null,
    val termId: String? = null,
    val studentsList: List<StudentScoreItem> = emptyList(),
    val error: String? = null,
    val isSuccess: Boolean = false // Set true when sync finishes
)

/**
 * Combines student details with their grading fields.
 * Validates inputs against limits organically in the UI.
 */
data class StudentScoreItem(
    val student: Student,
    var ca1: String = "",   // Stored as string for UI TextField binding
    var ca2: String = "",
    var exam: String = "",
    val confidence: Double? = null // Passed down if data came from OCR
) {
    // Computes dynamic total right on the device for immediate UI feedback.
    val total: Int
        get() = (ca1.toIntOrNull() ?: 0) + (ca2.toIntOrNull() ?: 0) + (exam.toIntOrNull() ?: 0)
        
    val hasWarning: Boolean
        get() = confidence != null && confidence < 0.85 // The 85% amber threshold from spec
}

/**
 * ViewModel for Teacher manual score entry and OCR review.
 */
@HiltViewModel
class ScoreEntryViewModel @Inject constructor(
    private val studentRepository: StudentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScoreUiState())
    val uiState: StateFlow<ScoreUiState> = _uiState.asStateFlow()

    fun loadClassScores(classId: String, subjectId: String, termId: String) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(isLoading = true, classId = classId, subjectId = subjectId, termId = termId) 
            }

            // Simplification: Load students by class and create empty score items
            studentRepository.getStudents().collect { result ->
                result.onSuccess { students ->
                    val classRoster = students.filter { it.classId == classId }
                    val scoreItems = classRoster.map { StudentScoreItem(student = it) }
                    
                    _uiState.update { 
                        it.copy(isLoading = false, studentsList = scoreItems)
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
     * Updates an individual student's score securely enforcing bounds.
     */
    fun updateStudentScore(studentId: String, field: String, value: String) {
        _uiState.update { currentState ->
            val updatedList = currentState.studentsList.map { item ->
                if (item.student.id == studentId) {
                    when (field) {
                        "ca1" -> item.copy(ca1 = limitValue(value, 20))
                        "ca2" -> item.copy(ca2 = limitValue(value, 20))
                        "exam" -> item.copy(exam = limitValue(value, 60))
                        else -> item
                    }
                } else item
            }
            currentState.copy(studentsList = updatedList)
        }
    }

    private fun limitValue(value: String, max: Int): String {
        if (value.isBlank()) return ""
        val intValue = value.toIntOrNull() ?: return ""
        if (intValue > max) return max.toString()
        if (intValue < 0) return "0"
        return intValue.toString()
    }

    /**
     * Attempts to save all marked records to the remote server.
     */
    fun submitScores() {
        val currentState = _uiState.value
        if (currentState.classId == null || currentState.studentsList.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // 1. Construct the bulk payload
            // In a real implementation this fires API via ScoreRepository -> ApiService.submitScoresBulk()
            
            // Assume network success for brevity:
            _uiState.update { it.copy(isLoading = false, isSuccess = true) }
        }
    }

    fun consumeError() { _uiState.update { it.copy(error = null) } }
}
