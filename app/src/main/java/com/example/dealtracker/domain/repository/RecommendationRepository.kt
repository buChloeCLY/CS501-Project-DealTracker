package com.example.dealtracker.domain.repository

import android.util.Log
import com.example.dealtracker.data.remote.repository.HistoryRepository
import com.example.dealtracker.domain.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository responsible for generating product recommendations based on user history.
 * @param historyRepository Repository for accessing user viewing history.
 * @param productRepository Repository for accessing product data.
 */
class RecommendationRepository(
    private val historyRepository: HistoryRepository,
    private val productRepository: ProductRepository
) {
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Retrieves a list of recommended products for a given user ID.
     * Recommendation logic: Based on the categories of the 5 most recently viewed products.
     * Recommended items are sorted by highest rating and then lowest price within those categories.
     * @param uid User ID.
     * @return List of recommended Product objects.
     */
    suspend fun getRecommendedProducts(uid: Int): List<Product> = withContext(Dispatchers.IO) {

        // ---------------------- 1. Retrieve viewing history ----------------------
        val historyResult = historyRepository.getUserHistory(uid)
        val historyList = historyResult.getOrElse {
            Log.e("RecoRepo", "Failed to load history", it)
            return@withContext emptyList()
        }

        if (historyList.isEmpty()) {
            return@withContext getFallbackProducts()
        }

        // ---------------------- 2. Sort by viewedAt ----------------------
        val sortedHistory = historyList.sortedByDescending { h ->
            try {
                sdf.parse(h.viewedAt)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }

        // Use the categories of the 5 most recently viewed products
        val recentPids = sortedHistory.take(5).map { it.pid }

        // ---------------------- 3. Fetch product categories ----------------------
        val categories = recentPids.mapNotNull { pid ->
            productRepository.getProductById(pid).getOrNull()?.category
        }.distinct()

        if (categories.isEmpty()) {
            return@withContext getFallbackProducts()
        }

        // ---------------------- 4. Get all products ----------------------
        val allProducts = productRepository.getAllProducts().getOrElse { emptyList() }

        // ---------------------- 5. Recommendation logic: Filter by category, sort by rating/price ----------------------
        val recommended = allProducts
            .filter { it.category in categories }
            .sortedWith(
                compareByDescending<Product> { it.rating }
                    .thenBy { it.price }
            )
            .take(10)

        return@withContext recommended
    }

    /**
     * Provides a list of highly-rated products as a fallback when history is unavailable.
     * @return List of top 10 highly-rated products.
     */
    private suspend fun getFallbackProducts(): List<Product> {
        return productRepository.getAllProducts()
            .getOrElse { emptyList() }
            .sortedByDescending { it.rating }
            .take(10)
    }
}