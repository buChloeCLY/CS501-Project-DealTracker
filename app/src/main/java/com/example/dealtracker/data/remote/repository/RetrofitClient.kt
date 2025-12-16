package com.example.dealtracker.data.remote.repository

import com.example.dealtracker.data.remote.api.ProducApi
import com.example.dealtracker.data.remote.api.PriceApi
import com.example.dealtracker.data.remote.api.UserApi
import com.example.dealtracker.data.remote.api.WishlistApi
import com.example.dealtracker.data.remote.api.HistoryApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit Client Configuration for connecting to the Node.js backend.
 * BASE_URL includes "/api/".
 */
object RetrofitClient {

    // Android Emulator loopback address to host machine
    private const val BASE_URL = "http://10.0.2.2:8080/api/"

    // Logging Interceptor for debugging network requests
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttp client with timeouts and logging
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Retrofit instance
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /**
     * Legacy API service for general database operations.
     */
    val api: ProducApi = retrofit.create(ProducApi::class.java)

    /**
     * API service for price-related endpoints.
     */
    val priceApi: PriceApi by lazy {
        retrofit.create(PriceApi::class.java)
    }

    /**
     * API service for user management endpoints (login, register, update).
     */
    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }

    /**
     * API service for wishlist management endpoints.
     */
    val wishlistApi: WishlistApi by lazy {
        retrofit.create(WishlistApi::class.java)
    }

    /**
     * API service for viewing history endpoints.
     */
    val historyApi: HistoryApi by lazy {
        retrofit.create(HistoryApi::class.java)
    }
}