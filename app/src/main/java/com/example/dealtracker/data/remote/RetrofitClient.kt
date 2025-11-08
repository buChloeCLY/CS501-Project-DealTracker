package com.example.dealtracker.data.remote

import com.example.dealtracker.data.remote.api.PriceApi
import com.example.dealtracker.data.remote.api.UserApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// ✅ Retrofit网络客户端单例
object RetrofitClient {
    // TODO: 根据实际环境切换base URL
    private const val BASE_URL = "http://10.0.0.133:5001/" // 开发环境

    // 懒加载API实例
    val priceApi: PriceApi by lazy {
        createRetrofit().create(PriceApi::class.java)
    }
    val userApi: UserApi by lazy {
        createRetrofit().create(UserApi::class.java)
    }
    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}