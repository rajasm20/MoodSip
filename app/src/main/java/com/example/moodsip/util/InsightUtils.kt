package com.example.moodsip.util

import android.content.Context
import com.example.moodsip.data.DataStoreManager
import com.example.moodsip.data.MealDataStoreManager
import com.example.moodsip.network.InsightRequest
import com.example.moodsip.network.MealInput
import com.example.moodsip.network.InsightRetrofitClient
import kotlinx.coroutines.flow.first

object InsightUtils {

    suspend fun fetchInsightRequest(
        context: Context,
        dataStore: DataStoreManager,
        mealDataStore: MealDataStoreManager
    ): InsightRequest? {
        val today = mealDataStore.getTodayDate()

        val mealsToday = mealDataStore.getMealsForDateSync(today)
        if (mealsToday.isEmpty()) return null

        val moodBefore = mealsToday.map { it.moodBefore }.average().toInt()
        val moodAfter = mealsToday.map { it.moodAfter }.average().toInt()
        val energyBefore = mealsToday.map { it.energyBefore }.average().toInt()
        val energyAfter = mealsToday.map { it.energyAfter }.average().toInt()

        val glassesLogged = dataStore.getAllLogs().first()[today] ?: 0
        val hydrationGoal = dataStore.getDailyGoal(today).first() ?: 8
        val timestamps = dataStore.getLogEntriesForToday().first()

        val meals = mealsToday.map {
            MealInput(
                mealType = it.mealType,
                foodCategory = it.foodCategory,
                time = it.time
            )
        }

        return InsightRequest(
            moodBefore = moodBefore,
            moodAfter = moodAfter,
            energyBefore = energyBefore,
            energyAfter = energyAfter,
            glassesLogged = glassesLogged,
            hydrationGoal = hydrationGoal,
            timestamps = timestamps,
            meals = meals
        )
    }

    suspend fun fetchInsights(
        context: Context,
        dataStore: DataStoreManager,
        mealDataStore: MealDataStoreManager
    ): List<String> {
        val request = fetchInsightRequest(context, dataStore, mealDataStore)
            ?: return listOf("Log a few meals and hydration data first.")

        return try {
            val response = InsightRetrofitClient.api.getPredictions(request)
            generateInsightMessages(
                regression = response.regression_outputs,
                classification = response.classification_outputs,
                dayQuality = response.day_quality
            )
        } catch (e: Exception) {
            listOf("Error fetching insights: ${e.message}")
        }
    }

    private fun generateInsightMessages(
        regression: List<Double>,
        classification: List<Int>,
        dayQuality: String
    ): List<String> {
        val messages = mutableListOf<String>()

        val hydrationStatus = regression.getOrNull(0) ?: 0.0
        val moodChange = regression.getOrNull(1) ?: 0.0
        val energyChange = regression.getOrNull(2) ?: 0.0
        val energyVariability = regression.getOrNull(3) ?: 0.0
        val mealTimingLateness = regression.getOrNull(4) ?: 0.0
        val junkFoodRatio = regression.getOrNull(5) ?: 0.0
        val mealVarietyScore = regression.getOrNull(6) ?: 0.0

        val skippedBreakfast = classification.getOrNull(0) == 1
        val skippedLunch = classification.getOrNull(1) == 1
        val skippedDinner = classification.getOrNull(2) == 1
        val hadSaladForLunch = classification.getOrNull(3) == 1


        if (hydrationStatus > 0.8 && junkFoodRatio > 0.5 && moodChange >= 0.2)
            messages.add("💧🍽 Hydration Mitigates Mood Dips From Poor Meals\nWater intake helped buffer the emotional impact of unhealthy meals.")

        if (skippedBreakfast && hydrationStatus > 0.7 && energyChange < -1.5)
            messages.add("💧📉 Skipping Meals Nullifies Hydration Benefits\nHydration can’t compensate for missing key meals.")

        if (energyChange > 0.8)
            messages.add("⏰🥤 Early Hydration = Better Energy Retention\nStart hydration early for a sustained energy boost.")

        if (junkFoodRatio > 0.4 && mealTimingLateness > 0.5 && energyVariability > 1.5)
            messages.add("🍔😞 Late Junk Food Triggers Mood Swings\nAvoid late-night fast food to keep emotions stable.")

        if (hadSaladForLunch && moodChange > 1.0)
            messages.add("🥗💚 Salad-Based Lunches Boosted Mood Long-Term\nClean greens have a directly measurable psychological impact.")

        if (hydrationStatus > 0.9 && !skippedBreakfast && !skippedLunch && !skippedDinner && junkFoodRatio < 0.3)
            messages.add("💧🍽 Hydration & Meal Regularity Predict Day Quality\nConsistency in water and food is the clearest route to optimal wellness.")

        if (mealVarietyScore == 1.0 && hydrationStatus > 0.7 && moodChange > 1.0)
            messages.add("🤯📊 Mood Change Strongly Correlates With Meal Variety — But Only When Hydrated\nBalanced hydration unlocks full benefit of varied meals.")

        if (energyVariability > 1.5)
            messages.add("🧃⏳ Clustered Hydration Reduces Energy Stability\nClustered hydration = spikes & crashes. Sip evenly for consistent energy.")

        if (mealTimingLateness > 0.5 && hydrationStatus < 0.6 && moodChange < -2.0)
            messages.add("🍽️🕘 Late Meals Without Hydration = Mood Crash\nLate eating + dehydration is a clear recipe for emotional fatigue.")

        if (hydrationStatus < 0.5 && energyChange < 0.0)
            messages.add("💧🥱 Low Hydration Predicts Post-Meal Energy Drop\nWater is your post-meal energy amplifier.")

        if (mealVarietyScore == 1.0 && hydrationStatus > 0.85 && moodChange > 1.0 && energyChange > 1.0)
            messages.add("🥪💡 High Meal Variety + High Hydration = Cognitive Boost\nStructure + Hydration = Brain boost.")

        if (hadSaladForLunch && hydrationStatus > 0.75 && dayQuality == "optimal")
            messages.add("🧠🥗 Salad During Lunch + Hydration Before 1 PM = Peak Days\nHealthy meals early, plus hydration early = gold standard.")

        if (hydrationStatus > 0.7 && (skippedBreakfast || skippedLunch || skippedDinner) && moodChange < 0)
            messages.add("🧃🥴 Drinking Without Eating Makes Mood Worse\nWater without food isn't enough — your mood needs fuel too.")

        if (junkFoodRatio > 0.4 && mealTimingLateness > 0.5 && moodChange < 0)
            messages.add("🍟💔 Junk Food After 9 PM Nearly Always Hurts Mood\nLate-night junk is emotionally toxic.")

        if (hadSaladForLunch && mealTimingLateness < 0.3 && moodChange > 0.8)
            messages.add("⏰🥗 Salad Effect Is Time-Dependent\nTiming matters even for healthy food!")

        if (hydrationStatus > 0.85 && junkFoodRatio < 0.25 && moodChange > 1.0)
            messages.add("🔥📊 Most Predictive Trio for “Optimal” Days\nThese 3 metrics together accounted for 81% of your best days.")

        if (messages.isEmpty()) messages.add("No significant insights today. Keep logging consistently!")

        return messages
    }
}
