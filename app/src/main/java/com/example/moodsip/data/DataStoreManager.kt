package com.example.moodsip.data

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Context.dataStore by preferencesDataStore("hydration_prefs")

class DataStoreManager(private val context: Context) {
    private val GLASS_COUNT_KEY = intPreferencesKey("glass_count")

    // Save today's glass count (for hydration tracking screen)
    suspend fun saveGlasses(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[GLASS_COUNT_KEY] = count
        }
    }

    fun getGlasses(): Flow<Int> {
        return context.dataStore.data.map { prefs ->
            prefs[GLASS_COUNT_KEY] ?: 0
        }
    }

    // Save glass count for a specific date (e.g., 2025-07-09)
    suspend fun saveDailyGlasses(date: String, count: Int) {
        context.dataStore.edit { prefs ->
            val key = stringPreferencesKey("daily_$date")
            prefs[key] = count.toString()
        }
    }

    // Get all daily logs from preferences (Map<yyyy-MM-dd, Int>)
    fun getAllLogs(): Flow<Map<String, Int>> {
        return context.dataStore.data.map { prefs ->
            prefs.asMap().mapNotNull { entry ->
                val keyName = entry.key.name
                if (keyName.startsWith("daily_")) {
                    val date = keyName.removePrefix("daily_")
                    val count = entry.value.toString().toIntOrNull()
                    if (count != null) date to count else null
                } else {
                    null
                }
            }.toMap()
        }
    }
    private fun logKeyForDate(date: String) = stringPreferencesKey("log_$date")

    suspend fun saveLogEntry(date: String, time: String) {
        context.dataStore.edit { prefs ->
            val key = logKeyForDate(date)
            val current = prefs[key] ?: ""
            prefs[key] = if (current.isBlank()) time else "$current,$time"
        }
    }

    fun getLogEntriesForToday(): Flow<List<String>> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val key = logKeyForDate(today)
        return context.dataStore.data.map { prefs ->
            prefs[key]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        }
    }
    suspend fun removeLastLogEntry(date: String) {
        context.dataStore.edit { prefs ->
            val key = logKeyForDate(date)
            val currentLog = prefs[key]
            if (!currentLog.isNullOrBlank()) {
                val entries = currentLog.split(",").filter { it.isNotBlank() }.toMutableList()
                if (entries.isNotEmpty()) {
                    entries.removeAt(entries.lastIndex)
                    prefs[key] = entries.joinToString(",")
                }
            }
        }
    }

    private fun goalKeyForDate(date: String) = stringPreferencesKey("goal_$date")

    suspend fun saveDailyGoal(date: String, goal: Int) {
        context.dataStore.edit { prefs ->
            prefs[goalKeyForDate(date)] = goal.toString()
        }
    }

    fun getDailyGoal(date: String): Flow<Int?> {
        return context.dataStore.data.map { prefs ->
            prefs[goalKeyForDate(date)]?.toIntOrNull()
        }
    }

}
