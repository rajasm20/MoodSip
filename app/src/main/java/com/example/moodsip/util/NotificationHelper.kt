package com.example.moodsip.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.moodsip.R

object NotificationHelper {
    private const val CHANNEL_ID = "hydration_reminders"

    fun showReminderNotification(context: Context, risk: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Hydration Reminders", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val message = when (risk.lowercase()) {
            "high" -> "🚨 High dehydration risk! Drink water now!"
            "medium" -> "⚠️ Don’t slow down now! Hydration is key to energy!"
            else -> "✅ Doing great! Keep up the streak!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Hydration Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(1, notification)
    }

    fun showFirstGlassNotification(context: Context) {
        showCustomNotification(context, "🌅 Good morning!", "Great start to your hydration journey today!")
    }

    fun showHalfwayNotification(context: Context) {
        showCustomNotification(context, "🥤 Halfway there!", "You’re doing great, keep sipping!")
    }

    fun showAlmostThereNotification(context: Context) {
        showCustomNotification(context, "🚰 Almost there!", "Just one more glass to reach your goal!")
    }

    private fun showCustomNotification(context: Context, title: String, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Hydration Reminders", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify((0..1000).random(), notification)
    }

    fun showHotWeatherNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "hydration_reminders",
                "Hydration Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "hydration_reminders")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔥 Heat Alert!")
            .setContentText("It's hot today! Your hydration goal increased to stay safe.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(1002, notification)
    }

}
