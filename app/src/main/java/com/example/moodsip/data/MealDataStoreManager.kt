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

    private fun keyForDate(date: String) = stringPreferencesKey("meal_logs_$date")

    fun getMealsForDate(date: String): Flow<List<MealEntry>> {
        val key = keyForDate(date)
        return context.dataStore.data.map { preferences ->
            preferences[key]?.let { json ->
                val type = object : TypeToken<List<MealEntry>>() {}.type
                gson.fromJson<List<MealEntry>>(json, type)
            } ?: emptyList()
        }
    }

    suspend fun saveMeal(entry: MealEntry) {
        val key = keyForDate(entry.date)
        context.dataStore.edit { preferences ->
            val existingJson = preferences[key]
            val currentList = if (existingJson != null) {
                val type = object : TypeToken<List<MealEntry>>() {}.type
                gson.fromJson<MutableList<MealEntry>>(existingJson, type)
            } else {
                mutableListOf()
            }
            currentList.add(entry)
            preferences[key] = gson.toJson(currentList)
        }
    }

    suspend fun deleteMeal(entry: MealEntry) {
        val key = keyForDate(entry.date)
        context.dataStore.edit { preferences ->
            val existingJson = preferences[key]
            if (existingJson != null) {
                val type = object : TypeToken<List<MealEntry>>() {}.type
                val list: List<MealEntry> = gson.fromJson(existingJson, type)
                val currentList = list.toMutableList()
                currentList.removeAll { it.time == entry.time && it.mealName == entry.mealName }
                preferences[key] = gson.toJson(currentList)
            }
        }
    }


    fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
    fun getDateDaysAgo(daysAgo: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -daysAgo)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

}
