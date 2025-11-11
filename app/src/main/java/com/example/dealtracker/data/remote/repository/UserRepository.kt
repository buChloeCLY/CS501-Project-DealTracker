package com.example.dealtracker.data.remote.repository

import com.example.dealtracker.data.remote.RetrofitClient
import com.example.dealtracker.data.remote.api.*
import com.example.dealtracker.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {

    suspend fun getUser(uid: Int): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.userApi.getUser(uid)
            if (response.isSuccessful && response.body() != null) {
                val userResponse = response.body()!!
                Result.success(
                    User(
                        uid = userResponse.uid,
                        name = userResponse.name,
                        email = userResponse.email,
                        gender = userResponse.gender
                    )
                )
            } else {
                Result.failure(Exception("Failed to get user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(
        uid: Int,
        name: String? = null,
        email: String? = null,
        gender: String? = null,
        password: String? = null
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val request = UserUpdateRequest(name, email, gender, password)
            val response = RetrofitClient.userApi.updateUser(uid, request)

            if (response.isSuccessful && response.body()?.success == true) {
                val userResponse = response.body()!!.user!!
                Result.success(
                    User(
                        uid = userResponse.uid,
                        name = userResponse.name,
                        email = userResponse.email,
                        gender = userResponse.gender
                    )
                )
            } else {
                Result.failure(Exception(response.body()?.error ?: "Update failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}