package com.example.dealtracker.domain.model

data class User(
    val uid: Int,
    val name: String,
    val email: String,
    val gender: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class UserUpdateRequest(
    val name: String? = null,
    val email: String? = null,
    val gender: String? = null,
    val password: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val gender: String = "Prefer not to say"
)

data class LoginResponse(
    val success: Boolean,
    val user: User? = null,
    val error: String? = null
)

data class RegisterResponse(
    val success: Boolean,
    val user: User? = null,
    val error: String? = null
)

data class UserResponse(
    val success: Boolean,
    val user: User? = null,
    val message: String? = null,
    val error: String? = null
)