package com.example.moodsip.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodsip.data.MealDataStoreManager
import com.example.moodsip.data.MealEntry
import com.example.moodsip.util.LLMMealEntry
import com.example.moodsip.network.MealInsightApi
import com.example.moodsip.network.LLMInsightApi
import com.example.moodsip.network.MealInsightLLMRequest
import com.example.moodsip.network.MealInsightRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MealInsightViewModel(private val dataStoreManager: MealDataStoreManager) : ViewModel() {

    private val _insightMessages = MutableStateFlow<List<String>>(emptyList())
    val insightMessages: StateFlow<List<String>> = _insightMessages

    private val api = MealInsightApi.create()
    private val llmApi = LLMInsightApi.create()

    fun generateInsights() {
        viewModelScope.launch {
            val allMeals = mutableListOf<MealEntry>()

            for (i in 0..6) {
                val date = dataStoreManager.getDateDaysAgo(i)
                val meals = dataStoreManager.getMealsForDate(date).first()
                allMeals.addAll(meals)
            }

            val recentMeals = allMeals.takeLast(5)
            val insights = mutableListOf<String>()

            // Local rule-based insights
            for (meal in recentMeals) {
                try {
                    val response = api.getInsights(
                        MealInsightRequest(
                            meal.mealType,
                            meal.mealName,
                            meal.foodCategory,
                            meal.time,
                            meal.moodBefore,
                            meal.moodAfter,
                            meal.energyBefore,
                            meal.energyAfter
                        )
                    )

                    when {
                        response.mood_trend == "increase" && response.energy_trend == "decrease" ->
                            insights.add("🥞 ${meal.mealName} boosted mood but left you tired later.")

                        response.mood_trend == "decrease" && response.energy_trend == "decrease" ->
                            insights.add("😢 ${meal.mealName} didn’t go well. Consider lighter options.")
                    }

                } catch (e: Exception) {
                    Log.e("INSIGHT_LOCAL_ERROR", "Local insight rule failed", e)
                }
            }

            // LLM-backed insights
            try {
                val llmMeals = recentMeals.map {
                    LLMMealEntry(
                        mealType = it.mealType,
                        mealName = it.mealName,
                        foodCategory = it.foodCategory,
                        time = it.time,
                        moodBefore = it.moodBefore,
                        moodAfter = it.moodAfter,
                        energyBefore = it.energyBefore,
                        energyAfter = it.energyAfter
                    )
                }

                val request = MealInsightLLMRequest(recent_meals = llmMeals)
                val response = llmApi.getLLMInsights(request)

                if (response.isSuccessful && response.body() != null) {
                    insights.addAll(response.body()!!.insights)
                } else {
                    Log.e("LLM_ERROR", "Failed: ${response.code()} ${response.errorBody()?.string()}")
                }

            } catch (e: Exception) {
                Log.e("INSIGHT_LLM_ERROR", "LLM call failed", e)
            }

            if (insights.isEmpty()) {
                insights.add("📊 No strong insights today — keep logging meals!")
            }

            _insightMessages.value = insights
        }
    }
}
