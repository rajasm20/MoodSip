package com.example.moodsip.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    val instance: PredictionService by lazy {
        Retrofit.Builder()
            .baseUrl(" https://hydration-api-1040726950593.europe-west1.run.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PredictionService::class.java)
    }
}
