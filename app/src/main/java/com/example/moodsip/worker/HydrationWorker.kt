package com.example.moodsip.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.moodsip.data.DataStoreManager
import com.example.moodsip.network.HydrationInput
import com.example.moodsip.network.RetrofitClient
import com.example.moodsip.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HydrationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val timeOfDay = when (hour) {
            in 5..11 -> "morning"
            in 12..17 -> "afternoon"
            else -> "evening"
        }

        try {
            val dataStore = DataStoreManager(applicationContext)
            val logs = dataStore.getAllLogs().first()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            var streak = 0
            var missed = 0
            var totalGlasses = 0f
            var daysCounted = 0

            val sortedDates = logs.keys.sortedDescending()
            for (date in sortedDates.take(7)) {
                val glasses = logs[date] ?: 0
                if (glasses > 0) {
                    streak++
                    totalGlasses += glasses
                    daysCounted++
                } else {
                    missed++
                    if (date < today) break // end streak on first missed past day
                }
            }
            val temp = inputData.getFloat("temperature", 25f) // default 25°C


            val avg = if (daysCounted > 0) totalGlasses / daysCounted else 0f

            val input = HydrationInput(
                streak = streak,
                avg_glasses = avg,
                missed_days = missed,
                temp = temp,
                time_of_day = timeOfDay
            )

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
