package com.example.dealtracker.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealtracker.data.remote.repository.HistoryRepository
import com.example.dealtracker.domain.model.History
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = false,
    val histories: List<History> = emptyList(),
    val error: String? = null
)

class HistoryViewModel : ViewModel() {

    private val repository = HistoryRepository()

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    /**
     * 加载用户浏览历史
     */
    fun loadHistory(uid: Int) {
        _uiState.value = HistoryUiState(isLoading = true)

        viewModelScope.launch {
            repository.getUserHistory(uid).fold(
                onSuccess = { histories ->
                    _uiState.value = HistoryUiState(
                        isLoading = false,
                        histories = histories
                    )
                },
                onFailure = { error ->
                    _uiState.value = HistoryUiState(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    /**
     * 删除单条历史记录
     */
    fun deleteHistory(hid: Int, uid: Int) {
        viewModelScope.launch {
            repository.deleteHistory(hid).fold(
                onSuccess = {
                    // 重新加载历史记录
                    loadHistory(uid)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * 清空所有历史记录
     */
    fun clearAllHistory(uid: Int) {
        viewModelScope.launch {
            repository.clearUserHistory(uid).fold(
                onSuccess = {
                    _uiState.value = HistoryUiState(
                        isLoading = false,
                        histories = emptyList()
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to clear history: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}