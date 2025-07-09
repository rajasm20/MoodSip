package com.example.moodsip
import androidx.work.workDataOf
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.widget.Toast
import java.util.concurrent.TimeUnit
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.work.OneTimeWorkRequestBuilder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.work.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.moodsip.data.DataStoreManager
import com.example.moodsip.worker.HydrationWorker
import com.example.moodsip.network.WeatherResponse
import com.example.moodsip.network.WeatherService
import com.example.moodsip.ui.theme.HydrationAppTheme
import com.example.moodsip.util.NotificationHelper
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var dataStore: DataStoreManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val analytics = Firebase.analytics
        dataStore = DataStoreManager(this)
        fun formatLogTime(raw: String): String {
            return try {
                val parser = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
                val time = parser.parse(raw.removePrefix("Drank at ")) ?: return raw
                "Drank at ${formatter.format(time)}"
            } catch (e: Exception) {
                raw
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        var latestTemperature: Float? = null

        setContent {
            HydrationAppTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                var glassCount by remember { mutableStateOf(0) }
                var hydrationGoal by remember { mutableStateOf(8) }
                val logList = remember { mutableStateListOf<String>() }
                val mediaPlayer = remember { MediaPlayer.create(context, R.raw.water_splash) }
                var temperature by remember { mutableStateOf<Float?>(null) }
                var streakCount by remember { mutableStateOf(0) }
                var lastLogDate by remember { mutableStateOf("") }

                // Fetch hydrationGoal from Remote Config and adjust with weather
                LaunchedEffect(true) {
                    val remoteConfig = Firebase.remoteConfig
                    remoteConfig.fetchAndActivate().await()
                    val baseGoal = remoteConfig.getLong("hydration_goal").toInt()

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
                        val tempAdjustment = ((temp - 25) / 5).toInt().coerceAtLeast(0)
                        hydrationGoal = baseGoal + tempAdjustment
                    } catch (e: Exception) {
                        Log.e("Weather", "Weather fetch failed", e)
                        hydrationGoal = baseGoal
                    }
                }

                // Load saved count
                LaunchedEffect(Unit) {
                    dataStore.getGlasses().collect {
                        glassCount = it
                    }
                }
                LaunchedEffect(true) {
                    dataStore.getLogEntriesForToday().collect { entries ->
                        logList.clear()
                        logList.addAll(entries.map { "Drank at $it" })
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE0F7FA)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Hydration Tracker", style = MaterialTheme.typography.headlineMedium)

                        temperature?.let {
                            Text("🌡 ${it.toInt()}°C in London — Goal: $hydrationGoal glasses", style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            for (i in 0 until hydrationGoal) {
                                val isFilled = i < glassCount
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable {
                                            val now = Calendar.getInstance()
                                            val currentHour = now.get(Calendar.HOUR_OF_DAY)
                                            val timeOfDay = when (currentHour) {
                                                in 5..11 -> "morning"
                                                in 12..17 -> "afternoon"
                                                else -> "evening"
                                            }
                                            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                                            if (!isFilled) {
                                                glassCount++
                                                scope.launch {
                                                    dataStore.saveGlasses(glassCount)
                                                    dataStore.saveDailyGlasses(today, glassCount)
                                                    dataStore.saveLogEntry(today, timestamp)
                                                }

                                                logList.add("Drank at $timestamp")
                                                mediaPlayer.start()

                                                when (glassCount) {
                                                    1 -> NotificationHelper.showFirstGlassNotification(context)
                                                    hydrationGoal / 2 -> NotificationHelper.showHalfwayNotification(context)
                                                    hydrationGoal - 1 -> NotificationHelper.showAlmostThereNotification(context)
                                                }

                                                val bundle = Bundle().apply {
                                                    putInt("glass_count", glassCount)
                                                    putString("time_of_day", timeOfDay)
                                                }
                                                analytics.logEvent("glass_logged", bundle)

                                                if (glassCount == hydrationGoal) {
                                                    if (lastLogDate != today) {
                                                        streakCount++
                                                        lastLogDate = today
                                                        analytics.logEvent("goal_reached", Bundle().apply {
                                                            putInt("streak_days", streakCount)
                                                        })
                                                    }
                                                }
                                            } else {
                                                glassCount--
                                                scope.launch {
                                                    dataStore.saveGlasses(glassCount)
                                                    dataStore.saveDailyGlasses(today, glassCount)
                                                    dataStore.removeLastLogEntry(today)
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(
                                            if (isFilled) R.drawable.glass_filled else R.drawable.glass_empty
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    if (isFilled) {
                                        Text("-", color = Color.Red, style = MaterialTheme.typography.bodyMedium)
                                    } else if (i == glassCount) {
                                        Text("+", color = Color.Blue, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LinearProgressIndicator(
                            progress = glassCount / hydrationGoal.toFloat(),
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Log", style = MaterialTheme.typography.titleMedium)

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(logList.size) { index ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD0F0FF)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text("💧", style = MaterialTheme.typography.bodyLarge)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = formatLogTime(logList[index]),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        val tempToSend = latestTemperature ?: 25f
        /*val workRequest = PeriodicWorkRequestBuilder<HydrationWorker>(
            6, TimeUnit.HOURS
        )
        .setInputData(workDataOf("temperature" to tempToSend))
        .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "hydration_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )*/

        val testRequest = OneTimeWorkRequestBuilder<HydrationWorker>()
            .setInputData(workDataOf("temperature" to tempToSend))
            .build()
        WorkManager.getInstance(this).enqueue(testRequest)
    }
}
