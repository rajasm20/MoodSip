package com.example.moodsip.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodsip.data.DataStoreManager
import com.example.moodsip.data.MealDataStoreManager
import com.example.moodsip.data.MealEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class HydrationStat(val date: String, val goal: Int, val consumed: Int)
data class MoodEnergyStat(val date: String, val mood: Float, val energy: Float)

@Composable
fun AnalyticsScreen(
    dataStore: DataStoreManager,
    mealDataStore: MealDataStoreManager
) {
    val scope = rememberCoroutineScope()
    var hydrationStats by remember { mutableStateOf<List<HydrationStat>>(emptyList()) }
    var moodEnergyStats by remember { mutableStateOf<List<MoodEnergyStat>>(emptyList()) }
    var mealLogs by remember { mutableStateOf<Map<String, List<MealEntry>>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val last5Days = (0L..4L).map {
                val calendar = Calendar.getInstance().apply { add(Calendar.DATE, -it.toInt()) }
                formatter.format(calendar.time)
            }.reversed()

            val hydration = last5Days.map { date ->
                val goal = dataStore.getHydrationGoal(date)
                val actual = dataStore.getHydrationCount(date)
                HydrationStat(date, goal, actual)
            }

            val allMeals = mutableMapOf<String, List<MealEntry>>()
            val moodEnergy = mutableListOf<MoodEnergyStat>()

            for (date in last5Days) {
                val meals = mealDataStore.getMealsForDateSync(date)
                allMeals[date] = meals

                if (meals.isNotEmpty()) {
                    val mood = meals.map { (it.moodBefore + it.moodAfter) / 2f }.average().toFloat()
                    val energy = meals.map { (it.energyBefore + it.energyAfter) / 2f }.average().toFloat()
                    moodEnergy.add(MoodEnergyStat(date, mood, energy))
                } else {
                    moodEnergy.add(MoodEnergyStat(date, 0f, 0f))
                }
            }

            hydrationStats = hydration
            moodEnergyStats = moodEnergy
            mealLogs = allMeals
            loading = false
        }
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFF9800))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF3E0)).padding(12.dp)) {

            item {
                Text("📅 Daily Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D4C41))
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(hydrationStats) { stat ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Date: ${stat.date}", fontWeight = FontWeight.Bold)
                        Text("Hydration Goal: ${stat.goal} glasses")
                        Text("Consumed: ${stat.consumed} glasses")

                        val meals = mealLogs[stat.date] ?: emptyList()
                        if (meals.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("🍽 Meals:", fontWeight = FontWeight.Bold)
                            meals.forEach {
                                Text("- ${it.mealType}: ${it.mealName} (${it.foodCategory})")
                            }
                        } else {
                            Text("No meals logged.")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text("📈 Mood & Energy Trends (Last 5 Days)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D4C41))
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(moodEnergyStats) { stat ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Date: ${stat.date}", fontWeight = FontWeight.Bold)
                        Text("Avg Mood: ${stat.mood}")
                        Text("Avg Energy: ${stat.energy}")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("🔥 Streak Tracker Coming Soon!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }
    }
}
