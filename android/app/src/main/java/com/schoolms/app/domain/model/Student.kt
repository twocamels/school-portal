package com.schoolms.app.domain.model

data class Student(
    val id: String,
    val firstName: String,
    val lastName: String,
    val admissionNumber: String,
    val classId: String? = null,
    val className: String? = null
)
