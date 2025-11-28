package com.example.dealtracker.ui.home.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Home 页面使用的 ViewModel，负责管理搜索栏文字与语音输入结果
 */
class HomeViewModel : ViewModel() {

    // 搜索栏文字
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    /** 更新搜索栏输入 */
    fun updateQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }

    /** 应用语音识别结果 */
    fun applyVoiceResult(text: String) {
        _searchQuery.value = text
    }
}
