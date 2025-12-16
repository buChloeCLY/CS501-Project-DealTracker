package com.example.dealtracker.data.remote.api

import retrofit2.Response
import retrofit2.http.*

interface UserApi {

    /**
     * Retrieves user details by ID.
     * @param uid User ID.
     */
    @GET("user/{uid}")
    suspend fun getUser(@Path("uid") uid: Int): Response<UserResponse>

    /**
     * Updates user information.
     * @param uid User ID.
     * @param request The fields to update.
     */
    @PUT("user/{uid}")
    suspend fun updateUser(
        @Path("uid") uid: Int,
        @Body request: UserUpdateRequest
    ): Response<UserUpdateResponse>

    /**
     * Logs in a user.
     * @param request User's email and password.
     */
    @POST("user/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /**
     * Registers a new user.
     * @param request New user's details.
     */
    @POST("user/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    /**
     * Deletes a user account.
     * @param uid User ID.
     */
    @DELETE("user/{uid}")
    suspend fun deleteUser(@Path("uid") uid: Int): Response<DeleteResponse>
}


data class UserUpdateRequest(
    val name: String? = null,
    val email: String? = null,
    val gender: String? = null,
    val password: String? = null
)

/**
 * Request body for user login.
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Request body for user registration.
 */
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val gender: String = "Prefer not to say"
)

data class UserResponse(
    val uid: Int,
    val name: String,
    val email: String,
    val gender: String,
    val created_at: String? = null,
    val updated_at: String? = null
)

/**
 * Response body for a user update operation.
 */
data class UserUpdateResponse(
    val success: Boolean,
    val user: UserResponse? = null,
    val error: String? = null,
    val message: String? = null
)

/**
 * Response body for a user login operation.
 */
data class LoginResponse(
    val success: Boolean,
    val user: UserResponse? = null,
    val error: String? = null
)

data class RegisterResponse(
    val success: Boolean,
    val user: UserResponse? = null,
    val error: String? = null
)

data class DeleteResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)