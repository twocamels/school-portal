package com.schoolms.app.domain.model

// Maps to Node.js backend envelope `{ data, meta, error }`
data class ApiResponse<T>(
    val data: T?,
    val meta: Map<String, Any>? = null,
    val error: ApiError? = null
)

data class ApiError(
    val message: String,
    val code: String
)

data class AuthResponse(
    val token: String,
    val user: User
)

data class User(
    val id: String,
    val email: String,
    val role: String,
    val organisationId: String?,
    val schoolId: String?
)

data class LoginRequest(
    val email: String,
    val password: String
)

// Simplified for Payment Screen Demo
data class Invoice(
    val id: String,
    val studentId: String,
    val amountDue: Double,
    val amountPaid: Double,
    val balance: Double,
    val studentName: String,
    val admissionNumber: String
)

data class PaymentRequest(
    val invoiceId: String,
    val amount: Double,
    val reference: String,
    val method: String = "bank_transfer",
    val date: String
)
