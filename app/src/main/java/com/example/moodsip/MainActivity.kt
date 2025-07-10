package com.example.moodsip

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.moodsip.ui.screens.HydrationScreen
import com.example.moodsip.ui.screens.ScreenDestination
import com.example.moodsip.ui.screens.CelebrationOverlay
import com.example.moodsip.ui.theme.HydrationAppTheme
import com.example.moodsip.worker.HydrationWorker
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.launch
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
            var selectedScreen by remember { mutableStateOf(ScreenDestination.HYDRATION) }
            val scope = rememberCoroutineScope()

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
                val workRequest = PeriodicWorkRequestBuilder<HydrationWorker>(6, TimeUnit.HOURS)
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                            ) {
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp
                                ) {
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_hydration),
                                                contentDescription = "Hydration",
                                                tint = if (selectedScreen == ScreenDestination.HYDRATION) Color(0xFF2196F3) else Color.Unspecified
                                            )
                                        },
                                        label = {
                                            Text(
                                                "Hydration",
                                                color = if (selectedScreen == ScreenDestination.HYDRATION) Color.Black else Color.Black
                                            )
                                        },
                                        selected = selectedScreen == ScreenDestination.HYDRATION,
                                        onClick = { selectedScreen = ScreenDestination.HYDRATION },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = Color(0xFFBBDEFB), // light blue halo
                                            selectedIconColor = Color(0xFF2196F3), // blue
                                            unselectedIconColor = Color.Unspecified,
                                            selectedTextColor = Color.Black,
                                            unselectedTextColor = Color.Black
                                        )
                                    )

                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_meal),
                                                contentDescription = "Meal",
                                                tint = if (selectedScreen == ScreenDestination.MEAL_LOG) Color(0xFF2196F3) else Color.Unspecified
                                            )
                                        },
                                        label = {
                                            Text(
                                                "Meal",
                                                color = if (selectedScreen == ScreenDestination.MEAL_LOG) Color.Black else Color.Black
                                            )
                                        },
                                        selected = selectedScreen == ScreenDestination.MEAL_LOG,
                                        onClick = { selectedScreen = ScreenDestination.MEAL_LOG },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = Color(0xFFBBDEFB),
                                            selectedIconColor = Color(0xFF2196F3),
                                            unselectedIconColor = Color.Unspecified,
                                            selectedTextColor = Color.Black,
                                            unselectedTextColor = Color.Black
                                        )
                                    )

                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_analytics),
                                                contentDescription = "Analytics",
                                                tint = if (selectedScreen == ScreenDestination.ANALYTICS) Color(0xFF2196F3) else Color.Unspecified
                                            )
                                        },
                                        label = {
                                            Text(
                                                "Analytics",
                                                color = if (selectedScreen == ScreenDestination.ANALYTICS) Color.Black else Color.Black
                                            )
                                        },
                                        selected = selectedScreen == ScreenDestination.ANALYTICS,
                                        onClick = { selectedScreen = ScreenDestination.ANALYTICS },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = Color(0xFFBBDEFB),
                                            selectedIconColor = Color(0xFF2196F3),
                                            unselectedIconColor = Color.Unspecified,
                                            selectedTextColor = Color.Black,
                                            unselectedTextColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }
                    }

                ) { paddingValues ->
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)) {

                        when (selectedScreen) {
                            ScreenDestination.HYDRATION -> HydrationScreen(
                                dataStore = dataStore,
                                temperature = temperature,
                                latestTemperature = latestTemperature,
                                onCelebration = { showCelebration = true }
                            )
                            ScreenDestination.MEAL_LOG -> {
                                Text("🍽 Meal Logging Coming Soon!", modifier = Modifier.align(Alignment.Center))
                            }
                            ScreenDestination.ANALYTICS -> {
                                Text("📊 Analytics Dashboard Coming Soon!", modifier = Modifier.align(Alignment.Center))
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
