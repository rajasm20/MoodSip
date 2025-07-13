package com.example.moodsip.network

import com.example.moodsip.util.LLMMealEntry

data class MealInsightLLMRequest(
    val recent_meals: List<LLMMealEntry>
)