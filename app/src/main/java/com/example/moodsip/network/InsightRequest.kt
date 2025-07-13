package com.example.moodsip.network

data class InsightRequest(
    val moodBefore: Int,
    val moodAfter: Int,
    val energyBefore: Int,
    val energyAfter: Int,
    val glassesLogged: Int,
    val hydrationGoal: Int,
    val timestamps: List<String>,
    val meals: List<MealInput>
)

data class MealInput(
    val mealType: String,
    val foodCategory: String,
    val time: String
)
