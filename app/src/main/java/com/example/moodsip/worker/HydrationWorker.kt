package com.example.moodsip.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.moodsip.network.HydrationInput
import com.example.moodsip.network.RetrofitClient
import com.example.moodsip.util.NotificationHelper
import java.util.*

class HydrationWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {

        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val timeOfDay = when (hour) {
            in 5..11 -> "morning"
            in 12..17 -> "afternoon"
            else -> "evening"
        }

        val input = HydrationInput(
            streak = 2,               // Replace with dynamic values
            avg_glasses = 5.5f,
            missed_days = 1,
            temp = 32f,
            time_of_day = timeOfDay
        )

        return try {
            val response = RetrofitClient.instance.predictRisk(input).execute()
            if (response.isSuccessful) {
                val risk = when (response.body()?.hydration_risk ?: 0) {
                    2 -> "high"
                    1 -> "medium"
                    else -> "low"
                }
                Log.d("HydrationWorker", "Prediction success: $risk")
                NotificationHelper.showReminderNotification(applicationContext, risk)
                Result.success()
            } else {
                Log.e("HydrationWorker", "Prediction failed: ${response.code()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("HydrationWorker", "Exception in prediction: ${e.message}", e)
            Result.failure()
        }
    }
}