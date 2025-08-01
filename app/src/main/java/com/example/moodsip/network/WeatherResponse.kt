package com.example.moodsip.network

data class ForecastResponse(val list: List<ForecastItem>)

data class ForecastItem(
    val dt_txt: String,
    val main: Main
)

data class Main(
    val temp: Float
)
