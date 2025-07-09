package com.example.moodsip.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call

data class HydrationInput(
    val streak: Int,
    val avg_glasses: Float,
    val missed_days: Int,
    val temp: Float,
    val time_of_day: String
)

data class PredictionResponse(val hydration_risk: Int)

interface PredictionService {
    @POST("/")
    fun predictRisk(@Body input: HydrationInput): Call<PredictionResponse>
}
