package com.example.moodsip

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.example.moodsip.data.DataStoreManager
import com.example.moodsip.network.WeatherService
import com.example.moodsip.ui.screens.HydrationScreen
import com.example.moodsip.ui.theme.HydrationAppTheme
import com.example.moodsip.util.NotificationHelper
import com.example.moodsip.worker.HydrationWorker
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var dataStore: DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataStore = DataStoreManager(this)
        val remoteConfig = Firebase.remoteConfig
        var latestTemperature: Float? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        setContent {
            var showCelebration by remember { mutableStateOf(false) }
            var temperature by remember { mutableStateOf<Float?>(null) }

            LaunchedEffect(Unit) {
                remoteConfig.fetchAndActivate().await()

                try {
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://api.openweathermap.org/data/2.5/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val service = retrofit.create(WeatherService::class.java)
                    val response = service.getWeather("London", "f7b60d5f4218e4937e14d28b42888bc5")
                    val temp = 35f
                    temperature = temp
                    latestTemperature = temp

                } catch (e: Exception) {
                    Log.e("Weather", "Fetch failed", e)
                    temperature = null
                }

                val tempToSend = latestTemperature ?: 25f
                val workRequest = PeriodicWorkRequestBuilder<HydrationWorker>(
                    6, TimeUnit.HOURS
                )
                    .setInputData(workDataOf("temperature" to tempToSend))
                    .build()

                WorkManager.getInstance(this@MainActivity).enqueueUniquePeriodicWork(
                    "hydration_reminder",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            }

            HydrationAppTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    HydrationScreen(
                        dataStore = dataStore,
                        temperature = temperature,
                        latestTemperature = latestTemperature,
                        onCelebration = { showCelebration = true }
                    )

                    if (showCelebration) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xAA000000))
                                .clickable { showCelebration = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text("🎉 Goal Reached!", style = MaterialTheme.typography.headlineSmall)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("You drank all your glasses today!", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { showCelebration = false }) {
                                        Text("Awesome!")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
