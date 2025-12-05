package com.example.dealtracker.domain.repository

import android.util.Log
import com.example.dealtracker.data.remote.repository.HistoryRepository
import com.example.dealtracker.domain.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class RecommendationRepository(
    private val historyRepository: HistoryRepository,
    private val productRepository: ProductRepository
) {
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    suspend fun getRecommendedProducts(uid: Int): List<Product> = withContext(Dispatchers.IO) {

        // ---------------------- 1. 获取浏览历史 ----------------------
        val historyResult = historyRepository.getUserHistory(uid)
        val historyList = historyResult.getOrElse {
            Log.e("RecoRepo", "Failed to load history", it)
            return@withContext emptyList()
        }

        if (historyList.isEmpty()) {
            return@withContext getFallbackProducts()
        }

        // ---------------------- 2. 按 viewedAt 排序 ----------------------
        val sortedHistory = historyList.sortedByDescending { h ->
            try {
                sdf.parse(h.viewedAt)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }

        // 最近 5 次浏览
        val recentPids = sortedHistory.take(5).map { it.pid }

        // ---------------------- 3. 查产品得到 category ----------------------
        val categories = recentPids.mapNotNull { pid ->
            productRepository.getProductById(pid).getOrNull()?.category
        }.distinct()

        if (categories.isEmpty()) {
            return@withContext getFallbackProducts()
        }

        // ---------------------- 4. 拿全部产品 ----------------------
        val allProducts = productRepository.getAllProducts().getOrElse { emptyList() }

        // ---------------------- 5. 推荐逻辑：同类 → 评分高 → 价格低 ----------------------
        val recommended = allProducts
            .filter { it.category in categories }
            .sortedWith(
                compareByDescending<Product> { it.rating }
                    .thenBy { it.price }
            )
            .take(10)

        return@withContext recommended
    }

    /** 当历史为空或查不到分类时，返回热门商品 */
    private suspend fun getFallbackProducts(): List<Product> {
        return productRepository.getAllProducts()
            .getOrElse { emptyList() }
            .sortedByDescending { it.rating }
            .take(10)
    }
}
