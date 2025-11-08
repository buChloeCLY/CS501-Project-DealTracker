package com.example.dealtracker.data.remote.api

import com.example.dealtracker.data.remote.api.UserUpdateRequest
import com.example.dealtracker.data.remote.api.UserUpdateResponse
import retrofit2.Response
import retrofit2.http.*

interface UserApi {

    @GET("user/{uid}")
    suspend fun getUser(@Path("uid") uid: Int): Response<UserResponse>

    @PUT("user/{uid}")
    suspend fun updateUser(
        @Path("uid") uid: Int,
        @Body request: UserUpdateRequest
    ): Response<UserUpdateResponse>

    @POST("user/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("user/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @DELETE("user/{uid}")
    suspend fun deleteUser(@Path("uid") uid: Int): Response<DeleteResponse>
}

// 请求数据类
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

// 响应数据类
data class UserResponse(
    val uid: Int,
    val name: String,
    val email: String,
    val gender: String,
    val created_at: String? = null,
    val updated_at: String? = null
)

data class UserUpdateResponse(
    val success: Boolean,
    val user: UserResponse? = null,
    val error: String? = null,
    val message: String? = null
)

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