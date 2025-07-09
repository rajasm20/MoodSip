package com.example.moodsip.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("hydration_prefs")

class DataStoreManager(private val context: Context) {
    private val GLASS_COUNT_KEY = intPreferencesKey("glass_count")

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
}
