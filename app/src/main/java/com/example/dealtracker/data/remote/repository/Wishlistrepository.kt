package com.example.dealtracker.data.remote.repository

import com.example.dealtracker.data.remote.api.WishlistApiService
import com.example.dealtracker.domain.model.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WishlistRepository {

    private val api: WishlistApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(WishlistApiService::class.java)
    }

    suspend fun getWishlist(uid: Int): Result<List<WishlistItem>> {
        return try {
            val response = api.getWishlist(uid)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToWishlist(
        uid: Int,
        pid: Int,
        targetPrice: Double? = null,
        alertEnabled: Boolean = true,
        priority: Int = 2,
        notes: String? = null
    ): Result<WishlistResponse> {
        return try {
            val request = AddWishlistRequest(
                uid = uid,
                pid = pid,
                target_price = targetPrice,
                alert_enabled = if (alertEnabled) 1 else 0,  // 只在这里转换
                priority = priority,
                notes = notes
            )
            val response = api.addToWishlist(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWishlist(
        wid: Int,
        targetPrice: Double? = null,
        alertEnabled: Boolean? = null,
        priority: Int? = null,
        notes: String? = null
    ): Result<WishlistResponse> {
        return try {
            val request = UpdateWishlistRequest(
                target_price = targetPrice,
                alert_enabled = alertEnabled?.let { if (it) 1 else 0 },  // 只在这里转换
                priority = priority,
                notes = notes
            )
            val response = api.updateWishlist(wid, request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromWishlist(wid: Int): Result<WishlistResponse> {
        return try {
            val response = api.removeFromWishlist(wid)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkWishlist(uid: Int, pid: Int): Result<CheckWishlistResponse> {
        return try {
            val response = api.checkWishlist(uid, pid)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAlerts(uid: Int): Result<AlertsResponse> {
        return try {
            val response = api.getAlerts(uid)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStats(uid: Int): Result<WishlistStats> {
        return try {
            val response = api.getStats(uid)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}