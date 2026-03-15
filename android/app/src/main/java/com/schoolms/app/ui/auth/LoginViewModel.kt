package com.schoolms.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolms.app.domain.model.LoginRequest
import com.schoolms.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State representing the UI for the LoginScreen.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false, // Signals navigation to the home route
    val roleLogged: String? = null // Passed to the navigator to select correct dashboard
)

/**
 * LoginViewModel coordinates the user's intent to log in with the AuthRepository.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Attempts to log the user in using the provided credentials.
     * Hides the keyboard and shows a loading spinner during the network call.
     */
    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(error = "Email and password cannot be empty.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = authRepository.login(LoginRequest(email, pass))
            
            result.onSuccess { response ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        // We extract the role dynamically to inform navigation
                        roleLogged = response.data?.user?.role
                    ) 
                }
            }.onFailure { exception ->
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "Login failed: ${exception.localizedMessage}"
                    ) 
                }
            }
        }
    }

    /**
     * Clears error states after they are shown in a Snackbar.
     */
    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }
}
