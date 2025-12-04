package com.example.dealtracker.data.remote.repository

import android.util.Log
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.domain.model.SearchResult
import com.example.dealtracker.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepositoryImpl : ProductRepository {

    private val api = RetrofitClient.api
    private val TAG = "ProductRepository"

    override suspend fun getAllProducts(): Result<List<Product>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getAllProducts()

                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    Result.success(list.map { it.toProduct() })
                } else {
                    Result.failure(Exception("API error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getProductById(pid: Int): Result<Product> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getProductById(pid)

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.toProduct())
                } else {
                    Result.failure(Exception("Product not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * ⭐ 分页搜索（唯一搜索接口）
     */
    override suspend fun searchProductsPaged(
        query: String,
        page: Int,
        size: Int
    ): Result<SearchResult> = withContext(Dispatchers.IO) {

        try {
            val response = api.searchProducts(query, page, size)

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Search failed: ${response.code()}"))
            }

            val body = response.body()
                ?: return@withContext Result.failure(Exception("Empty response"))

            val products = body.products.map { it.toProduct() }

            Result.success(
                SearchResult(
                    products = products,
                    page = body.page,
                    totalPages = body.totalPages,
                    total = body.total
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProductsByPlatform(platform: String): Result<List<Product>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getByPlatform(platform)

                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    Result.success(list.map { it.toProduct() })
                } else {
                    Result.failure(Exception("Platform filter failed"))
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getProductsByPriceRange(minPrice: Double, maxPrice: Double): Result<List<Product>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getByPriceRange(minPrice, maxPrice)

                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    Result.success(list.map { it.toProduct() })
                } else {
                    Result.failure(Exception("Price filter failed"))
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
