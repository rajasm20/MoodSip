package com.example.moodsip.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodsip.data.MealDataStoreManager
import com.example.moodsip.data.MealEntry
import com.example.moodsip.network.MealInsightApi
import com.example.moodsip.network.MealInsightRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MealInsightViewModel(private val dataStoreManager: MealDataStoreManager) : ViewModel() {

    private val _insightMessages = MutableStateFlow<List<String>>(emptyList())
    val insightMessages: StateFlow<List<String>> = _insightMessages

    private val api = MealInsightApi.create()

    fun generateInsights() {
        viewModelScope.launch {
            val allMeals = mutableListOf<MealEntry>()

            for (i in 0..6) { // last 7 days
                val date = dataStoreManager.getDateDaysAgo(i)
                val meals = dataStoreManager.getMealsForDate(date).first()
                allMeals.addAll(meals)
            }

            val insights = mutableListOf<String>()
            val grouped = allMeals.groupBy { it.date }

            // 1. Streaks for healthy meals
            val healthyStreak = grouped.values.count { day ->
                day.all { it.foodCategory in listOf("Salads and Healthy Bowls", "Home-Cooked") }
            }
            if (healthyStreak >= 3) insights.add("🥗 Great job! You've eaten healthy for $healthyStreak days in a row!")

            // 2. Skipped breakfasts
            val skippedBreakfasts = grouped.values.count { meals ->
                meals.none { it.mealType == "Breakfast" }
            }
            if (skippedBreakfasts > 0) insights.add("⏰ Skipped breakfast $skippedBreakfasts times. Breakfast fuels your day!")

            // 3. Dessert alert
            val dessertCount = allMeals.count { it.foodCategory == "Desserts and Sweets" }
            if (dessertCount >= 3) insights.add("🍰 You've had desserts $dessertCount times recently. Sweet but keep an eye!")

            // 4. Frequent junk/snack meals
            val junkCount = allMeals.count { it.foodCategory == "Junk Snacks" || it.foodCategory == "Fast Food & Take Out" }
            if (junkCount >= 5) insights.add("🛒 Too many junk/snack meals ($junkCount)! Try to balance with whole meals.")

            // 5. High mood boosters
            val bestMoodMeals = allMeals.filter { it.moodAfter - it.moodBefore >= 2 }
            if (bestMoodMeals.isNotEmpty()) insights.add("😄 Meals that lifted your mood: ${bestMoodMeals.take(3).joinToString { it.mealName }}")

            // 6. Low energy dips
            val dips = allMeals.filter { it.energyAfter < it.energyBefore }
            if (dips.size >= 3) insights.add("😫 ${dips.size} meals led to energy dips. Watch what drains you.")

            // 7. Predictive insights from model
            val recentMeals = allMeals.takeLast(5)
            for (meal in recentMeals) {
                try {
                    val response = api.getInsights(
                        MealInsightRequest(
                            meal.mealType, meal.mealName, meal.foodCategory, meal.time,
                            meal.moodBefore, meal.moodAfter, meal.energyBefore, meal.energyAfter
                        )
                    )

                    when {
                        response.mood_trend == "increase" && response.energy_trend == "decrease" ->
                            insights.add("🥞 ${meal.mealName} boosted mood but left you tired later.")

                        response.mood_trend == "decrease" && response.energy_trend == "decrease" ->
                        insights.add("😢 ${meal.mealName} didn’t go well. Consider lighter options.")
                    }
                } catch (e: Exception) {

                }
            }

            if (insights.isEmpty()) insights.add("📊 No strong insights today — keep logging meals!")
            _insightMessages.value = insights
        }
    }
}
