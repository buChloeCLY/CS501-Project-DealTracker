package com.example.dealtracker.data.remote

import com.example.dealtracker.data.remote.api.PriceApi
import com.example.dealtracker.data.remote.api.UserApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

//  Retrofit网络客户端单例
// 连接 Flask 后端（价格API）,对应app.py, 端口5001, 调用api接口PriceApi, UserApi
object RetrofitClient {
    // TODO: 根据实际环境切换base URL
    private const val BASE_URL = "http://10.0.0.133:5001/" // 开发环境
//    private const val BASE_URL = "http://10.0.0.231:5001/" // cly开发环境
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