package com.schoolms.app.domain.repository

import com.schoolms.app.data.remote.ApiService
import com.schoolms.app.domain.model.ApiResponse
import com.schoolms.app.domain.model.AuthResponse
import com.schoolms.app.domain.model.LoginRequest
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthRepository acts as the single source of truth for authentication data.
 * It coordinates network calls via ApiService and local storage via TokenManager.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    /**
     * Authenticate a user and store their token on success.
     * We return the parsed ApiResponse directly to the ViewModel.
     */
    suspend fun login(request: LoginRequest): Result<ApiResponse<AuthResponse>> {
        return try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                
                // If the backend returned a token in the data block, save it
                // We're expecting { data: { token: '...', user: {...} }, meta: null, error: null }
                apiResponse.data?.token?.let { token ->
                    tokenManager.saveAccessToken(token)
                }

                Result.success(apiResponse)
            } else {
                // Here we would typically parse the error body according to our { error: { message, code } } envelope
                // For brevity, we are returning a generic failure if not successful
                Result.failure(Exception("Login failed with status ${response.code()}"))
            }
        } catch (e: Exception) {
            // Catches network errors, timeouts, etc.
            Result.failure(e)
        }
    }

    /**
     * Terminate the session by clearing local storage.
     */
    suspend fun logout() {
        tokenManager.clearAccessToken()
    }

    /**
     * A helper to check if a user is currently logged in.
     */
    suspend fun isLoggedIn(): Boolean {
        return !tokenManager.getAccessToken().firstOrNull().isNullOrBlank()
    }
}
