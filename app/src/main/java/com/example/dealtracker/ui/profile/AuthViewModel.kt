package com.example.dealtracker.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealtracker.data.local.UserPreferences
import com.example.dealtracker.data.remote.repository.RetrofitClient
import com.example.dealtracker.data.remote.api.LoginRequest
import com.example.dealtracker.data.remote.api.RegisterRequest
import com.example.dealtracker.domain.UserManager
import com.example.dealtracker.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val userApi = RetrofitClient.userApi

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * 登录
     */
    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val request = LoginRequest(email, password)
                val response = userApi.login(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val userResponse = response.body()!!.user!!
                    val user = User(
                        uid = userResponse.uid,
                        name = userResponse.name,
                        email = userResponse.email,
                        gender = userResponse.gender
                    )

                    // 保存到全局状态
                    UserManager.setUser(user)

                    // 保存到持久化存储
                    UserPreferences.saveUser(user)

                    onSuccess()
                } else {
                    val errorMsg = response.body()?.error ?: "Login failed"
                    _error.value = errorMsg
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Network error"
                _error.value = errorMsg
                onError(errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 注册
     */
    fun register(
        name: String,
        email: String,
        password: String,
        gender: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val request = RegisterRequest(name, email, password, gender)
                val response = userApi.register(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val userResponse = response.body()!!.user!!
                    val user = User(
                        uid = userResponse.uid,
                        name = userResponse.name,
                        email = userResponse.email,
                        gender = userResponse.gender
                    )

                    // 保存到全局状态
                    UserManager.setUser(user)

                    // 保存到持久化存储
                    UserPreferences.saveUser(user)

                    onSuccess()
                } else {
                    val errorMsg = response.body()?.error ?: "Registration failed"
                    _error.value = errorMsg
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Network error"
                _error.value = errorMsg
                onError(errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _error.value = null
    }
}