package com.example.moodsip.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class MealInsightRequest(
    val mealType: String,
    val mealName: String,
    val foodCategory: String,
    val time: String,
    val moodBefore: Int,
    val moodAfter: Int,
    val energyBefore: Int,
    val energyAfter: Int
)

data class MealInsightResponse(
    val mood_trend: String,
    val energy_trend: String
)

interface MealInsightApi {
    @POST("/")
    suspend fun getInsights(@Body request: MealInsightRequest): MealInsightResponse

    companion object {
        fun create(): MealInsightApi {
            return Retrofit.Builder()
                .baseUrl("MEAL_INSIGHT_API")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MealInsightApi::class.java)
        }
    }
}
