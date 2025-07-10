// HydrationScreen.kt

package com.example.moodsip.ui.screens
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.moodsip.R
import com.example.moodsip.data.DataStoreManager
import com.example.moodsip.util.NotificationHelper
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HydrationScreen(
    dataStore: DataStoreManager,
    temperature: Float?,
    latestTemperature: Float?,
    onCelebration: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var glassCount by remember { mutableStateOf(0) }
    var hydrationGoal by remember { mutableStateOf(8) }
    val logList = remember { mutableStateListOf<String>() }
    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.water_splash) }
    var streakCount by remember { mutableStateOf(0) }
    var lastLogDate by remember { mutableStateOf("") }

    val analytics = Firebase.analytics

    LaunchedEffect(Unit) {
        dataStore.getGlasses().collect { glassCount = it }
    }
    LaunchedEffect(true) {
        dataStore.getLogEntriesForToday().collect { entries ->
            logList.clear()
            logList.addAll(entries.map { "Drank at $it" })
        }
    }

    temperature?.let {
        val baseGoal = 8
        val tempAdjustment = ((it - 25) / 5).toInt().coerceAtLeast(0)
        hydrationGoal = baseGoal + tempAdjustment
        if (hydrationGoal >= 10) NotificationHelper.showHotWeatherNotification(context)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE0F7FA)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Hydration Tracker", style = MaterialTheme.typography.headlineMedium)
            temperature?.let {
                Text("\uD83C\uDF21 ${it.toInt()}\u00B0C — Goal: $hydrationGoal glasses",
                    style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                for (i in 0 until hydrationGoal) {
                    val isFilled = i < glassCount
                    Box(
                        modifier = Modifier.size(48.dp).clickable {
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
                                analytics.logEvent("glass_logged", Bundle().apply {
                                    putInt("glass_count", glassCount)
                                    putString("time_of_day", timeOfDay)
                                })
                                if (glassCount == hydrationGoal) {
                                    if (lastLogDate != today) {
                                        streakCount++
                                        lastLogDate = today
                                        analytics.logEvent("goal_reached", Bundle().apply {
                                            putInt("streak_days", streakCount)
                                        })
                                    }
                                    onCelebration()
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
                            Text("\uD83D\uDCA7", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = logList[index],
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
