package com.example.dealtracker.data.remote.repository

import com.example.dealtracker.data.remote.api.DatabaseApiService
import com.example.dealtracker.data.remote.api.PriceApi
import com.example.dealtracker.data.remote.api.UserApi
import com.example.dealtracker.data.remote.api.WishlistApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 客户端配置
 *
 * 连接 Node.js 后端（server.js, 端口 8080）
 * BASE_URL 已经带了 "/api/"，所以接口定义里只写 "user/xxx" / "wishlist" 等，
 * 不要再加 "api/" 前缀。
 */
object RetrofitClient {

    // Android 模拟器访问本机: http://10.0.2.2:8080/api/
    private const val BASE_URL = "http://10.0.2.2:8080/api/"

    // Logging Interceptor（用于调试，查看网络请求）
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttp 客户端配置
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Retrofit 实例
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /**
     * 旧的总入口（如果项目里之前用的是 DatabaseApiService 就继续保留）
     * 例如：RetrofitClient.api.xxx()
     */
    val api: DatabaseApiService = retrofit.create(DatabaseApiService::class.java)

    // 价格 API (原来的 Flask API，现在在 Node.js 中)
    val priceApi: PriceApi by lazy {
        retrofit.create(PriceApi::class.java)
    }

    /**
     * 用户相关接口：UserApi
     * 例如：RetrofitClient.userApi.login(...)
     */
    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }

    /**
     * 新增：Wishlist 相关接口
     * 例如：RetrofitClient.wishlistApi.getAlerts(uid)
     */
    val wishlistApi: WishlistApi by lazy {
        retrofit.create(WishlistApi::class.java)
    }
}
