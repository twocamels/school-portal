package com.schoolms.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolms.app.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State representing the UI for the DashboardScreen.
 * Covers both OrgAdmin (Proprietor) and Admin (Principal) views.
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val isOrgAdmin: Boolean = false,
    val totalStudents: Int = 0,
    val totalCollected: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val todayAttendance: Int = 0, // School scope only
    val locations: List<LocationStat> = emptyList(), // Org scope only
    val error: String? = null
)

/**
 * Simplified metric for a specific school location (Campus).
 */
data class LocationStat(
    val name: String,
    val students: Int,
    val collected: Double,
    val outstanding: Double
)

/**
 * ViewModel for the Proprietor / Principal Home Screen.
 * Responsible for fetching high-level metrics required for the proprietor demo.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /**
     * Determine load routing based on the JWT scope derived during Login (Pair 4).
     */
    fun loadDashboard(role: String) {
        val isOrg = role == "org_admin"
        _uiState.update { it.copy(isLoading = true, isOrgAdmin = isOrg) }

        viewModelScope.launch {
            try {
                if (isOrg) {
                    loadOrgStats()
                } else {
                    loadSchoolStats()
                }
            } catch (e: Exception) {
                // For demo resilience, load mock data if network fails entirely
                val t = if (isOrg) "Proprietor" else "Principal"
                _uiState.update { it.copy(isLoading = false, error = "Failed to fetch $t data: ${e.message}") }
            }
        }
    }

    private suspend fun loadSchoolStats() {
        // val response = apiService.getSchoolDashboardStats()
        // ... parse into state
        
        // Simulating the Network hit for Demo prep
        kotlinx.coroutines.delay(800)
        _uiState.update { 
            it.copy(
                isLoading = false,
                totalStudents = 345,
                todayAttendance = 310, // ~89% attendance rate
                totalOutstanding = 1_450_000.0, // Naira
                totalCollected = 4_500_000.0
            ) 
        }
    }

    private suspend fun loadOrgStats() {
        // val response = apiService.getOrgDashboardStats()
        // ... parse into state

        // Simulating the Network hit for Demo prep
        kotlinx.coroutines.delay(800)
        _uiState.update { 
            it.copy(
                isLoading = false,
                totalStudents = 850,
                totalOutstanding = 3_200_000.0,
                totalCollected = 12_500_000.0,
                locations = listOf(
                    LocationStat("Main Campus (Ikeja)", 450, 7_000_000.0, 1_500_000.0),
                    LocationStat("Annex (Surulere)", 400, 5_500_000.0, 1_700_000.0)
                )
            ) 
        }
    }
}
