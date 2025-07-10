package com.example.moodsip.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

data class MealEntry(
    val date: String,
    val time: String,
    val mealType: String,
    val mealName: String,
    val foodCategory: String,
    val moodBefore: Int,
    val moodAfter: Int,
    val energyBefore: Int,
    val energyAfter: Int
)

class MealDataStoreManager(private val context: Context) {
    private val Context.dataStore by preferencesDataStore(name = "meal_logs")
    private val gson = Gson()

    fun getMealsForDate(date: String): Flow<List<MealEntry>> {
        val key = stringPreferencesKey("meal_logs_$date")
        return context.dataStore.data.map { preferences ->
            preferences[key]?.let { json ->
                val type = object : TypeToken<List<MealEntry>>() {}.type
                gson.fromJson(json, type)
            } ?: emptyList()
        }
    }

    suspend fun saveMeal(entry: MealEntry) {
        val key = stringPreferencesKey("meal_logs_${entry.date}")
        context.dataStore.edit { preferences ->
            val existingJson = preferences[key]
            val currentList = if (existingJson != null) {
                val type = object : TypeToken<List<MealEntry>>() {}.type
                gson.fromJson<List<MealEntry>>(existingJson, type).toMutableList()
            } else {
                mutableListOf()
            }
            currentList.add(entry)
            preferences[key] = gson.toJson(currentList)
        }
    }

    fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}
