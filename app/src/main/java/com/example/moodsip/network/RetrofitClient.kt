package com.example.moodsip.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    val instance: PredictionService by lazy {
        Retrofit.Builder()
            .baseUrl("HYDRATION_API_URL")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PredictionService::class.java)
    }
}
