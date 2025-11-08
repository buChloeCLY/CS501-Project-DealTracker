package com.example.dealtracker.data.remote.repository

import android.util.Log
import com.example.dealtracker.domain.model.Product
import com.example.dealtracker.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 产品仓库实现
 * 负责从后端 API 获取数据
 */
class ProductRepositoryImpl : ProductRepository {

    private val api = RetrofitClient.api
    private val TAG = "ProductRepository"

    /**
     * 获取所有产品
     */
    override suspend fun getAllProducts(): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📦 Fetching all products from API...")

            val response = api.getAllProducts()

            if (response.isSuccessful) {
                val productDTOs = response.body()

                if (productDTOs != null && productDTOs.isNotEmpty()) {
                    val products = productDTOs.map { it.toProduct() }
                    Log.d(TAG, "✅ Successfully fetched ${products.size} products")
                    Result.success(products)
                } else {
                    Log.w(TAG, "⚠️ No products found in response")
                    Result.success(emptyList())
                }
            } else {
                val error = "API Error: ${response.code()} - ${response.message()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch products", e)
            Result.failure(e)
        }
    }

    /**
     * 根据 ID 获取产品
     */
    override suspend fun getProductById(pid: Int): Result<Product> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Fetching product with pid=$pid")

            val response = api.getProductById(pid)

            if (response.isSuccessful && response.body() != null) {
                val product = response.body()!!.toProduct()
                Log.d(TAG, "✅ Found product: ${product.title}")
                Result.success(product)
            } else {
                val error = "Product not found: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch product", e)
            Result.failure(e)
        }
    }

    /**
     * 搜索产品
     */
    override suspend fun searchProducts(query: String): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Searching products: query='$query'")

            val response = api.searchProducts(query)

            if (response.isSuccessful) {
                val products = response.body()?.map { it.toProduct() } ?: emptyList()
                Log.d(TAG, "✅ Found ${products.size} products")
                Result.success(products)
            } else {
                val error = "Search failed: ${response.code()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Search error", e)
            Result.failure(e)
        }
    }

    /**
     * 按平台筛选
     */
    override suspend fun getProductsByPlatform(platform: String): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📦 Fetching products for platform: $platform")

            val response = api.getByPlatform(platform)

            if (response.isSuccessful) {
                val products = response.body()?.map { it.toProduct() } ?: emptyList()
                Log.d(TAG, "✅ Found ${products.size} products")
                Result.success(products)
            } else {
                Result.failure(Exception("Failed to filter by platform"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Filter error", e)
            Result.failure(e)
        }
    }

    /**
     * 按价格区间筛选
     */
    override suspend fun getProductsByPriceRange(
        minPrice: Double,
        maxPrice: Double
    ): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📦 Fetching products in price range: $$minPrice - $$maxPrice")

            val response = api.getByPriceRange(minPrice, maxPrice)

            if (response.isSuccessful) {
                val products = response.body()?.map { it.toProduct() } ?: emptyList()
                Log.d(TAG, "✅ Found ${products.size} products")
                Result.success(products)
            } else {
                Result.failure(Exception("Failed to filter by price"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Filter error", e)
            Result.failure(e)
        }
    }
}