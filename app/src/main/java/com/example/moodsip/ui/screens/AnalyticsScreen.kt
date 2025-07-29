package com.example.moodsip.ui.screens
import android.util.Log
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.moodsip.data.DataStoreManager
import com.example.moodsip.data.MealDataStoreManager
import com.example.moodsip.data.MealEntry
import com.example.moodsip.ui.components.ChartMarkerView
import com.example.moodsip.util.InsightUtils
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    dataStore: DataStoreManager,
    mealDataStore: MealDataStoreManager,
    temperature: Float? = null
) {
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
        selection = CalendarSelection.Date { date -> selectedDate = date },
        config = CalendarConfig(
            monthSelection = true,
            yearSelection = true,
            boundary = LocalDate.MIN..LocalDate.now()
        )
    )

    val startOfWeek = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
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
        color = Color(0xFFF6F9FC)
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
                            text = if (hydrationCount >= hydrationGoal) "🏃 Goal met!" else "🥤 Keep sipping!",
                            fontSize = 14.sp,
                            color = if (hydrationCount >= hydrationGoal) Color(0xFF388E3C) else Color(0xFF1976D2),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                                    Text("📌 ${entry.foodCategory}", fontSize = 12.sp)
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
            AnalyticsCharts(
                dataStore = dataStore,
                mealDataStore = mealDataStore
            )
            StreakCardSection(
                dataStore = dataStore,
                mealDataStore = mealDataStore
            )

        }
    }
}



@Composable
fun LineChartFixed(
    title: String,
    entries: List<Entry>,
    labels: List<String>,
    selectedRange: Int
) {
    if (entries.isEmpty()) return
    val lineColor = when (title) {
        "Mood" -> AndroidColor.parseColor("#F48FB1")
        "Energy" -> AndroidColor.parseColor("#FFD54F")
        else -> AndroidColor.parseColor("#1976D2")
    }

    AndroidView(factory = { context ->
        LineChart(context).apply {
            val dataSet = LineDataSet(entries, "").apply {
                color = lineColor
                setCircleColor(lineColor)
                lineWidth = 2f
                circleRadius = 4f
                valueTextSize = 10f
                setDrawValues(false)

                if (selectedRange == 28) {
                    setDrawCircles(false)
                    setDrawHighlightIndicators(true)
                    highLightColor = AndroidColor.RED
                } else {
                    setDrawCircles(true)
                    setDrawHighlightIndicators(true)
                    highLightColor = AndroidColor.RED
                }
            }

            this.data = LineData(dataSet)
            this.description = Description().apply { text = "" }
            this.legend.isEnabled = false

            this.xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(labels)
                textColor = AndroidColor.BLACK
                labelRotationAngle = 315f
            }

            this.axisLeft.apply {
                setDrawGridLines(true)
                textColor = AndroidColor.BLACK
            }

            this.axisRight.isEnabled = false

            val markerColor = when (title) {
                "Mood" -> "#F48FB1"
                "Energy" -> "#fae8af"
                else -> "#1976D2"
            }
            val marker = ChartMarkerView(context, labels, title, markerColor)
            marker.chartView = this
            this.marker = marker

            this.animateX(1000)
        }
    }, modifier = Modifier
        .fillMaxWidth()
        .height(300.dp))
}



@Composable
fun AnalyticsCharts(
    dataStore: DataStoreManager,
    mealDataStore: MealDataStoreManager
) {
    var selectedChartType by remember { mutableStateOf("Hydration") }
    var selectedRange by remember { mutableStateOf(7) }
    var expanded by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val labelFormatter = DateTimeFormatter.ofPattern("MMM d")

    val entries = remember { mutableStateListOf<Entry>() }
    val labels = remember { mutableStateListOf<String>() }

    val chartEmoji = when (selectedChartType) {
        "Hydration" -> "\uD83D\uDCCA"
        "Mood" -> "\uD83D\uDE0A"
        "Energy" -> "\u26A1"
        else -> ""
    }

    LaunchedEffect(selectedChartType, selectedRange) {
        try {
            entries.clear()
            labels.clear()

            val allHydrationLogs = dataStore.getAllLogs().first()
            val allMealLogs = if (selectedChartType != "Hydration")
                mealDataStore.getMealsForLastNDays(selectedRange)
            else emptyMap()

            for (i in 0 until selectedRange) {
                val date = today.minusDays((selectedRange - 1 - i).toLong())
                val dateStr = date.format(dateFormatter)
                val label = date.format(labelFormatter)

                val rawValue: Float = when (selectedChartType) {
                    "Hydration" -> allHydrationLogs[dateStr]?.toFloat() ?: 0f
                    "Mood" -> {
                        val meals = allMealLogs[dateStr] ?: emptyList()
                        if (meals.isNotEmpty())
                            meals.map { (it.moodBefore + it.moodAfter) / 2.0 }
                                .average().toFloat()
                        else 0f
                    }
                    "Energy" -> {
                        val meals = allMealLogs[dateStr] ?: emptyList()
                        if (meals.isNotEmpty())
                            meals.map { (it.energyBefore + it.energyAfter) / 2.0 }
                                .average().toFloat()
                        else 0f
                    }
                    else -> 0f
                }

                val safeValue = if (rawValue.isFinite()) rawValue else 0f
                entries.add(Entry(i.toFloat(), safeValue))

                val finalLabel = when {
                    selectedRange <= 7 -> label
                    selectedRange <= 14 -> if (i % 2 == 0) label else ""
                    else -> if (i % 4 == 0 || i == selectedRange - 1) label else ""
                }
                labels.add(finalLabel)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //Wrap chart section in Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            //Title as Dropdown Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.clickable { expanded = true }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$chartEmoji $selectedChartType",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = "▼",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("Hydration", "Mood", "Energy").forEach { option ->
                            val emoji = when (option) {
                                "Hydration" -> "\uD83D\uDCCA"
                                "Mood" -> "\uD83D\uDE0A"
                                "Energy" -> "\u26A1"
                                else -> ""
                            }

                            DropdownMenuItem(
                                text = { Text("$emoji $option") },
                                onClick = {
                                    selectedChartType = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Range Selector for chart, modify later
                Row {
                    listOf(7 to "1W", 14 to "2W", 28 to "4W").forEach { (days, label) ->
                        val selected = selectedRange == days
                        Text(
                            text = label,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clickable { selectedRange = days }
                                .background(
                                    color = if (selected) Color(0xFF1976D2) else Color.White,
                                    shape = RoundedCornerShape(50)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) Color(0xFF1976D2) else Color.LightGray,
                                    shape = RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                            color = if (selected) Color.White else Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // chart itself
            LineChartFixed(
                title = selectedChartType,
                entries = entries,
                labels = labels,
                selectedRange = selectedRange
            )
        }
    }
}

@Composable
fun StreakCardSection(
    dataStore: DataStoreManager,
    mealDataStore: MealDataStoreManager
) {
    var selectedIcon by remember { mutableStateOf(0) }
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    var hydrationStreak by remember { mutableStateOf(0) }
    var mealStreak by remember { mutableStateOf(0) }

    val context = LocalContext.current
    var insightsList by remember { mutableStateOf(listOf<String>()) }
    var insightLoaded by remember { mutableStateOf(false) }

    // Fetch hydration and meal streaks
    LaunchedEffect(Unit) {
        val hydrationLogs = dataStore.getAllLogs().first()
        hydrationStreak = (0..30).takeWhile { offset ->
            val date = today.minusDays(offset.toLong()).format(formatter)
            val count = hydrationLogs[date] ?: 0
            val goal = dataStore.getDailyGoal(date).first() ?: 8
            count >= goal
        }.count()

        mealStreak = (0..30).takeWhile { offset ->
            val date = today.minusDays(offset.toLong()).format(formatter)
            val meals = mealDataStore.getMealsForDateSync(date)
            val typesLogged = meals.map { it.mealType }.distinct()
            listOf("Breakfast", "Lunch", "Snack", "Dinner").all { it in typesLogged }
        }.count()
    }


    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedIcon) {
        if (selectedIcon == 2 && !insightLoaded) {
            scope.launch {
                val allInsights = InsightUtils.fetchInsights(context, dataStore, mealDataStore)
                insightsList = allInsights.shuffled().take(2)
                insightLoaded = true
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2331)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                when (selectedIcon) {
                    0 -> {
                        Text("💧", fontSize = 28.sp)
                        Text("$hydrationStreak day streak", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            if (hydrationStreak > 0) "Yay! You're crushing your hydration goals!" else "Start sipping to build your streak!",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                    1 -> {
                        Text("🍽️", fontSize = 28.sp)
                        Text("$mealStreak day streak", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            if (mealStreak > 0) "Full meals logged daily. Great job!" else "Log all 4 meal types to build your streak!",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                    2 -> {
                        Text("💡", fontSize = 28.sp)
                        Text("Today’s Insights", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)

                        Spacer(modifier = Modifier.height(8.dp))

                        if (insightLoaded) {
                            insightsList.forEach { insight ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = insight.split("\n")[0],
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = insight.split("\n").drop(1).joinToString("\n"),
                                        fontSize = 12.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Fetching your mood & energy tips...",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                        }
                    }

                }
            }

            // Right icon column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 12.dp)
            ) {
                IconButton(
                    onClick = { selectedIcon = 0 },
                    modifier = Modifier
                        .size(42.dp)
                        .background(if (selectedIcon == 0) Color.White else Color.Transparent, RoundedCornerShape(50))
                ) {
                    Text("💧")
                }
                IconButton(
                    onClick = { selectedIcon = 1 },
                    modifier = Modifier
                        .size(42.dp)
                        .background(if (selectedIcon == 1) Color.White else Color.Transparent, RoundedCornerShape(50))
                ) {
                    Text("🍽️")
                }
                IconButton(
                    onClick = { selectedIcon = 2 },
                    modifier = Modifier
                        .size(42.dp)
                        .background(if (selectedIcon == 2) Color.White else Color.Transparent, RoundedCornerShape(50))
                ) {
                    Text("💡")
                }
            }
        }
    }
}











