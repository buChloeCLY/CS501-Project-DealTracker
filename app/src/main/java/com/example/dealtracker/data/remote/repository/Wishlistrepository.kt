package com.example.dealtracker.data.remote.repository

import com.example.dealtracker.data.remote.api.WishlistItemResponse
import com.example.dealtracker.data.remote.api.WishlistUpsertRequest
import com.example.dealtracker.data.remote.api.WishlistUpsertResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WishlistRepository {

    // ⭐ 添加获取 Wishlist 的方法
    suspend fun getWishlist(uid: Int): Result<List<WishlistItemResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val resp = RetrofitClient.wishlistApi.getWishlist(uid)
                val body = resp.body()
                if (resp.isSuccessful && body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Get wishlist failed: ${resp.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun upsertWishlist(
        uid: Int,
        pid: Int,
        targetPrice: Double?,
        alertEnabled: Boolean = true,
        notes: String? = null,
        priority: Int? = 2
    ): Result<WishlistUpsertResponse> = withContext(Dispatchers.IO) {
        try {
            val resp = RetrofitClient.wishlistApi.upsertWishlist(
                WishlistUpsertRequest(
                    uid = uid,
                    pid = pid,
                    target_price = targetPrice,
                    alert_enabled = alertEnabled,
                    notes = notes,
                    priority = priority
                )
            )
            val body = resp.body()
            if (resp.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Wishlist upsert failed: ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWishlist(uid: Int, pid: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val resp = RetrofitClient.wishlistApi.deleteWishlist(
                    mapOf("uid" to uid, "pid" to pid)
                )
                if (resp.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Wishlist delete failed: ${resp.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getAlerts(uid: Int): Result<List<WishlistItemResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val resp = RetrofitClient.wishlistApi.getAlerts(uid)
                val body = resp.body()
                if (resp.isSuccessful && body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Get alerts failed: ${resp.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ⭐ 标记为已推送
    suspend fun markNotified(uid: Int, pid: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val resp = RetrofitClient.wishlistApi.markNotified(
                    mapOf("uid" to uid, "pid" to pid)
                )
                if (resp.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Mark notified failed: ${resp.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ⭐ 标记为已读
    suspend fun markRead(uid: Int, pid: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val resp = RetrofitClient.wishlistApi.markRead(
                    mapOf("uid" to uid, "pid" to pid)
                )
                if (resp.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Mark read failed: ${resp.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}