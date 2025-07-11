package com.example.moodsip.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodsip.data.DataStoreManager
import com.example.moodsip.data.MealDataStoreManager
import com.example.moodsip.data.MealEntry
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    dataStore: DataStoreManager,
    mealDataStore: MealDataStoreManager,
    temperature: Float? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var mealLog by remember { mutableStateOf<List<MealEntry>>(emptyList()) }
    var hydrationCount by remember { mutableStateOf(0) }
    var hydrationGoal by remember { mutableStateOf(8) }
    val calendarState = rememberUseCaseState()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    CalendarDialog(
        state = calendarState,
        selection = CalendarSelection.Date { date ->
            selectedDate = date
        },
        config = CalendarConfig(
            monthSelection = true,
            yearSelection = true,
            boundary = LocalDate.MIN..LocalDate.now()
        )
    )

    val startOfWeek = today.minusDays(today.dayOfWeek.value % 7L)
    val weekDates = (0..6).map { startOfWeek.plusDays(it.toLong()) }
    val formatterDay = DateTimeFormatter.ofPattern("E")
    val formatterDate = DateTimeFormatter.ofPattern("d")
    val fullDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

    LaunchedEffect(selectedDate) {
        val selectedDateStr = selectedDate.format(formatter)
        val allLogs = dataStore.getAllLogs().first()
        hydrationCount = allLogs[selectedDateStr] ?: 0

        mealLog = mealDataStore.getMealsForDate(selectedDateStr).first()
            .sortedBy { it.time }

        dataStore.getDailyGoal(selectedDateStr).collect { savedGoal ->
            hydrationGoal = savedGoal ?: 8
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF6F9FC) // Light background for Analytics tab
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (selectedDate == today) "Today" else selectedDate.format(fullDateFormatter),
                    modifier = Modifier.clickable { calendarState.show() },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDates.forEach { date ->
                    val isSelected = date == selectedDate
                    val isFuture = date.isAfter(today)

                    val bgColor = when {
                        isSelected -> Color(0xFFBBDEFB)
                        isFuture -> Color.LightGray
                        else -> Color.Transparent
                    }

                    val borderColor = if (isFuture) Color.Gray else Color(0xFF1976D2)
                    val textColor = if (isFuture) Color.Gray else Color.Black

                    Column(
                        modifier = Modifier
                            .width(48.dp)
                            .height(64.dp)
                            .background(color = bgColor, shape = RoundedCornerShape(50))
                            .border(
                                border = BorderStroke(2.dp, borderColor),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable(enabled = !isFuture) { selectedDate = date },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = date.format(formatterDay).first().toString(),
                            fontSize = 12.sp,
                            color = textColor
                        )
                        Text(
                            text = date.format(formatterDate),
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Hydration Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "💧 $hydrationCount / $hydrationGoal glasses",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (hydrationCount >= hydrationGoal) "🎯 Goal met!" else "🥤 Keep sipping!",
                            fontSize = 14.sp,
                            color = if (hydrationCount >= hydrationGoal) Color(0xFF388E3C) else Color(0xFF1976D2),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Meal Log
                    if (mealLog.isEmpty()) {
                        Text("🍽 No meals logged on this day.", color = Color.Gray)
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "🍱 Meal Log",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        mealLog.forEach { entry ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "🍽 ${entry.mealType} - ${entry.mealName}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "🕒 ${entry.time}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text("\uD83D\uDCCC ${entry.foodCategory}", fontSize = 12.sp)
                                    Text(
                                        "😊 Mood: ${entry.moodBefore} ➔ ${entry.moodAfter}    ⚡ Energy: ${entry.energyBefore} ➔ ${entry.energyAfter}",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Reserved space for charts
        }
    }
}

