package com.example.moodsip.util

data class LLMMealEntry(
    val mealType: String,
    val mealName: String,
    val foodCategory: String,
    val time: String,
    val moodBefore: Int,
    val moodAfter: Int,
    val energyBefore: Int,
    val energyAfter: Int
)