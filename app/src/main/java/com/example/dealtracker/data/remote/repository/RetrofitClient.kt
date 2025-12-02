package com.example.dealtracker.data.remote

import com.example.dealtracker.data.remote.api.PriceApi
import com.example.dealtracker.data.remote.api.UserApi
import com.example.dealtracker.data.remote.api.WishlistApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.example.dealtracker.data.remote.api.DatabaseApiService

/**
 * 统一的 Retrofit 客户端
 * 连接 Node.js 后端 (server.js, 端口 8080)
 * 整合所有 API: 价格、用户、心愿单、产品等
 */
object RetrofitClient {

    // Node.js 后端地址
    // Android 模拟器访问本机: http://10.0.2.2:8080/
    // 真机访问电脑: http://你的电脑IP:8080/ (如 http://192.168.1.100:8080/)
    private const val BASE_URL = "http://10.0.2.2:8080/"

    // Logging Interceptor（用于调试）
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttp 客户端配置
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Retrofit 实例
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // 价格 API (原来的 Flask API，现在在 Node.js 中)
    val priceApi: PriceApi by lazy {
        retrofit.create(PriceApi::class.java)
    }

    // 用户 API (原来的 Flask API，现在在 Node.js 中)
    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }

    // 心愿单 API (新增)
    val wishlistApi: WishlistApiService by lazy {
        retrofit.create(WishlistApiService::class.java)
    }

    val api: DatabaseApiService = retrofit.create(DatabaseApiService::class.java)
}