package com.example.moodsip.network

import retrofit2.http.Body
import retrofit2.http.POST

interface InsightApi {
    @POST("predict")
    suspend fun getPredictions(@Body request: InsightRequest): InsightResponse
}
