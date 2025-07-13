package com.example.moodsip.network

data class InsightResponse(
    val regression_outputs: List<Double>,
    val classification_outputs: List<Int>,
    val day_quality: String
)
