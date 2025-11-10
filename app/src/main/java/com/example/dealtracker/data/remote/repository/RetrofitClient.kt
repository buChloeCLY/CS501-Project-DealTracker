package com.example.dealtracker.data.remote.repository

import com.example.dealtracker.data.remote.api.DatabaseApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

//连接 Node.js 后端（主数据库API）, 对应server.js, 端口8080，调用api接口DatabaseApiService
/**
 * Retrofit 客户端配置
 */
object RetrofitClient {

    // 后端 API 地址
    // Android 模拟器访问本机: http://10.0.2.2:8080/api/
    // 真机访问电脑: http://你的电脑IP:8080/api/ (如 http://192.168.1.100:8080/api/)
    // 部署后: http://your-server.com/api/
    private const val BASE_URL = "http://10.0.2.2:8080/api/"

    // Logging Interceptor（用于调试，查看网络请求）
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY  // 打印完整请求和响应
    }

    // OkHttp 客户端配置
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)   // 连接超时
        .readTimeout(30, TimeUnit.SECONDS)      // 读取超时
        .writeTimeout(30, TimeUnit.SECONDS)     // 写入超时
        .build()

    // Retrofit 实例
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // API 服务实例
    val api: DatabaseApiService = retrofit.create(DatabaseApiService::class.java)
}