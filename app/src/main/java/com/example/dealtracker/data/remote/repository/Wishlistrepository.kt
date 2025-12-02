package com.example.dealtracker.data.remote.repository

import com.example.dealtracker.data.remote.api.WishlistItemResponse
import com.example.dealtracker.data.remote.api.WishlistUpsertRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WishlistRepository {

    suspend fun upsertWishlist(
        uid: Int,
        pid: Int,
        targetPrice: Double?,
        alertEnabled: Boolean = true,
        notes: String? = null,
        priority: Int? = 2
    ): Result<Unit> = withContext(Dispatchers.IO) {
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
            if (resp.isSuccessful) {
                Result.success(Unit)
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
}
