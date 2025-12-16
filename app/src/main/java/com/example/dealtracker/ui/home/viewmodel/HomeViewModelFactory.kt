package com.example.dealtracker.ui.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dealtracker.domain.repository.RecommendationRepository

class HomeViewModelFactory(
    private val recommendationRepository: RecommendationRepository,
    private val userId: Int
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                recommendationRepository = recommendationRepository,
                userId = userId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
