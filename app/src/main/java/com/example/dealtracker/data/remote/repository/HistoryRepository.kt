package com.example.dealtracker.data.remote.repository

import android.util.Log
import com.example.dealtracker.data.remote.api.AddHistoryRequest
import com.example.dealtracker.domain.model.History
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryRepository {

    private val api = RetrofitClient.historyApi
    private val TAG = "HistoryRepository"

    /**
     * 获取用户浏览历史
     */
    suspend fun getUserHistory(uid: Int): Result<List<History>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching history for user: $uid")
            val response = api.getUserHistory(uid)

            if (response.isSuccessful && response.body() != null) {
                val histories = response.body()!!.map { it.toHistory() }
                Log.d(TAG, "Successfully fetched ${histories.size} history records")
                Result.success(histories)
            } else {
                val error = "Failed to fetch history: ${response.code()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching history", e)
            Result.failure(e)
        }
    }

    /**
     * 添加浏览记录
     */
    suspend fun addHistory(uid: Int, pid: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Adding history: uid=$uid, pid=$pid")
            val request = AddHistoryRequest(uid, pid)
            val response = api.addHistory(request)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "History added successfully")
                Result.success(true)
            } else {
                val error = response.body()?.message ?: "Failed to add history"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding history", e)
            Result.failure(e)
        }
    }

    /**
     * 删除单条历史记录
     */
    suspend fun deleteHistory(hid: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting history: hid=$hid")
            val response = api.deleteHistory(hid)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "History deleted successfully")
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete history"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting history", e)
            Result.failure(e)
        }
    }

    /**
     * 清空用户所有历史记录
     */
    suspend fun clearUserHistory(uid: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Clearing all history for user: $uid")
            val response = api.clearUserHistory(uid)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "All history cleared successfully")
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to clear history"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing history", e)
            Result.failure(e)
        }
    }
}