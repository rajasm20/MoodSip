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
            "medium" -> "💧 You're halfway. Stay on track."
            else -> "✅ Doing great! Keep up the streak!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Replace with your actual icon
            .setContentTitle("Hydration Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(1, notification)
    }
}
