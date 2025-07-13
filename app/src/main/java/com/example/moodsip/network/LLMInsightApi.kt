package com.example.moodsip.network


import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Headers
import retrofit2.Response

interface LLMInsightApi {
    @Headers("Content-Type: application/json")
    @POST("meal-insights/")
    suspend fun getLLMInsights(@Body request: MealInsightLLMRequest): Response<LLMInsightResponse>

    companion object {
        fun create(): LLMInsightApi {
            val retrofit = retrofit2.Retrofit.Builder()
                .baseUrl("https://llm-meal-api-1040726950593.europe-west1.run.app/")
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build()
            return retrofit.create(LLMInsightApi::class.java)
        }
    }
}
