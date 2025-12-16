package com.example.dealtracker.data.remote.api

import retrofit2.Response
import retrofit2.http.*

interface HistoryApi {

    /**
     * Retrieves the user's viewing history.
     * @param uid User ID.
     */
    @GET("view-history/{uid}")
    suspend fun getUserHistory(@Path("uid") uid: Int): Response<List<HistoryDto>>

    /**
     * Adds a new viewing history record.
     * @param request The request body containing the history details.
     */
    @POST("view-history")
    suspend fun addHistory(@Body request: AddHistoryRequest): Response<AddHistoryResponse>

    /**
     * Deletes a single history record.
     * @param hid History record ID.
     */
    @DELETE("view-history/{hid}")
    suspend fun deleteHistory(@Path("hid") hid: Int): Response<AddHistoryResponse>

    /**
     * Clears all viewing history for a user.
     * @param uid User ID.
     */
    @DELETE("view-history/user/{uid}")
    suspend fun clearUserHistory(@Path("uid") uid: Int): Response<AddHistoryResponse>
}