package com.example.page3.network

import com.example.page3.model.PriceDto
import retrofit2.http.GET
import retrofit2.http.Path

interface PriceApi {
    @GET("price/{pid}")
    suspend fun getPrices(@Path("pid") pid: Int): List<PriceDto>
}
