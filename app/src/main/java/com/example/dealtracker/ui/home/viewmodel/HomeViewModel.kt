package com.example.dealtracker.ui.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen, managing recommendations and search input state.
 * @param recommendationRepository Repository for fetching personalized product recommendations.
 * @param userId The ID of the currently logged-in user.
 */
class HomeViewModel(
    private val recommendationRepository: RecommendationRepository,
    private val userId: Int   // Logged-in user ID
) : ViewModel() {

    // State flow for recommended products
    private val _recommendedProducts = MutableStateFlow<List<Product>>(emptyList())
    val recommendedProducts: StateFlow<List<Product>> = _recommendedProducts

    // State flow for the current search query text
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // State flow for voice input errors
    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError

    init {
        loadRecommendations()
    }

    /** Loads recommended products based on user ID and history. */
    fun loadRecommendations() {
        viewModelScope.launch {
            val list = recommendationRepository.getRecommendedProducts(userId)
            _recommendedProducts.value = list
        }
    }

    /**
     * Sets an error message related to voice recognition.
     * @param msg The error message string.
     */
    fun setVoiceError(msg: String) {
        _voiceError.value = msg
    }

    /**
     * Clears the current voice error after it has been shown to the user.
     * Prevents the same Snackbar from showing repeatedly on recomposition.
     */
    fun clearVoiceError() {
        _voiceError.value = null
    }


    /**
     * Updates the search bar input text.
     * @param newQuery The new query string.
     */
    fun updateQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }

    /**
     * Applies the result of voice recognition to the search bar.
     * @param text The transcribed text from voice input.
     */
    fun applyVoiceResult(text: String) {
        _searchQuery.value = text
    }
}