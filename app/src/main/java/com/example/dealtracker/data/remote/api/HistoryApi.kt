package com.example.dealtracker.data.remote.api

import retrofit2.Response
import retrofit2.http.*

interface HistoryApi {

    /**
     * 获取用户的浏览历史
     */
    @GET("view-history/{uid}")
    suspend fun getUserHistory(@Path("uid") uid: Int): Response<List<HistoryDto>>

    /**
     * 添加浏览记录
     */
    @POST("view-history")
    suspend fun addHistory(@Body request: AddHistoryRequest): Response<AddHistoryResponse>

    /**
     * 删除单条历史记录
     */
    @DELETE("view-history/{hid}")
    suspend fun deleteHistory(@Path("hid") hid: Int): Response<AddHistoryResponse>

    /**
     * 清空用户所有历史记录
     */
    @DELETE("view-history/user/{uid}")
    suspend fun clearUserHistory(@Path("uid") uid: Int): Response<AddHistoryResponse>
}