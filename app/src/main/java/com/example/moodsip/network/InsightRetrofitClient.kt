package com.example.moodsip.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object InsightRetrofitClient {
    private const val BASE_URL = "ANALYTICS_API_URL"

    val api: InsightApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(InsightApi::class.java)
    }
}
