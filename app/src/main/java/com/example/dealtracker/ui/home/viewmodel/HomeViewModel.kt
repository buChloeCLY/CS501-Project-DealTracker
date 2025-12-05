package com.example.dealtracker.ui.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val recommendationRepository: RecommendationRepository,
    private val userId: Int   // 登录后的用户 id
) : ViewModel() {

    // 推荐数据
    private val _recommendedProducts = MutableStateFlow<List<Product>>(emptyList())
    val recommendedProducts: StateFlow<List<Product>> = _recommendedProducts

    // 搜索栏文字
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError

    init {
        loadRecommendations()
    }

    /** 加载推荐商品 */
    fun loadRecommendations() {
        viewModelScope.launch {
            val list = recommendationRepository.getRecommendedProducts(userId)
            _recommendedProducts.value = list
        }
    }

    fun setVoiceError(msg: String) {
        _voiceError.value = msg
    }

    /** 更新搜索栏输入 */
    fun updateQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }

    /** 应用语音识别结果 */
    fun applyVoiceResult(text: String) {
        _searchQuery.value = text
    }
}