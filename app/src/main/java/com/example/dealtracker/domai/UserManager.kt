package com.example.dealtracker.domain

import com.example.dealtracker.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserManager {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun setUser(user: User?) {
        _currentUser.value = user
    }

    fun getUser(): User? = _currentUser.value

    fun isLoggedIn(): Boolean = _currentUser.value != null

    fun logout() {
        _currentUser.value = null
    }
}