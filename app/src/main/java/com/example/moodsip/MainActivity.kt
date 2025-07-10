package com.example.moodsip

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.example.moodsip.data.DataStoreManager
import com.example.moodsip.network.WeatherService
import com.example.moodsip.ui.screens.CelebrationOverlay
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

enum class BottomNavDestination { HYDRATION, MEAL_LOG, ANALYTICS }

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
            var selectedTab by remember { mutableStateOf(BottomNavDestination.HYDRATION) }

            LaunchedEffect(Unit) {
                remoteConfig.fetchAndActivate().await()
                try {
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://api.openweathermap.org/data/2.5/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val service = retrofit.create(WeatherService::class.java)
                    val response = service.getWeather("London", "f7b60d5f4218e4937e14d28b42888bc5")
                    val temp = response.main.temp
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
                Scaffold(
                    bottomBar = {
                        NavigationBar(containerColor = Color.LightGray) {
                            NavigationBarItem(
                                icon = { Icon(painter = painterResource(id = R.drawable.ic_hydration), contentDescription = "Hydration") },
                                label = { Text("Hydration") },
                                selected = selectedTab == BottomNavDestination.HYDRATION,
                                onClick = { selectedTab = BottomNavDestination.HYDRATION }
                            )
                            NavigationBarItem(
                                icon = { Icon(painter = painterResource(id = R.drawable.ic_meal), contentDescription = "Meal Log") },
                                label = { Text("Meals") },
                                selected = selectedTab == BottomNavDestination.MEAL_LOG,
                                onClick = { selectedTab = BottomNavDestination.MEAL_LOG }
                            )
                            NavigationBarItem(
                                icon = { Icon(painter = painterResource(id = R.drawable.ic_analytics), contentDescription = "Analytics") },
                                label = { Text("Stats") },
                                selected = selectedTab == BottomNavDestination.ANALYTICS,
                                onClick = { selectedTab = BottomNavDestination.ANALYTICS }
                            )
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)) {
                        when (selectedTab) {
                            BottomNavDestination.HYDRATION -> HydrationScreen(
                                dataStore = dataStore,
                                temperature = temperature,
                                latestTemperature = latestTemperature,
                                onCelebration = { showCelebration = true }
                            )
                            BottomNavDestination.MEAL_LOG -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text("🍽 Meal Logger Coming Soon")
                            }
                            BottomNavDestination.ANALYTICS -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text("📊 Analytics Coming Soon")
                            }
                        }
                        if (showCelebration) {
                            CelebrationOverlay { showCelebration = false }
                        }
                    }
                }
            }

        }
    }
}
