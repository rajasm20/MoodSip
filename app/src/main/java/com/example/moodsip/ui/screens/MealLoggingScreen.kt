package com.example.moodsip.ui.screens

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.moodsip.data.MealDataStoreManager
import com.example.moodsip.data.MealEntry
import com.example.moodsip.data.dataStore
import com.example.moodsip.viewModel.MealInsightViewModel
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealLoggerScreen(mealDataStoreManager: MealDataStoreManager) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val analytics = Firebase.analytics

    var selectedMealType by remember { mutableStateOf("Breakfast") }
    var selectedFoodCategory by remember { mutableStateOf("Home-Cooked") }
    var showMealTypeDialog by remember { mutableStateOf(false) }
    var showFoodCategoryDialog by remember { mutableStateOf(false) }
    var showMoodInfo by remember { mutableStateOf(false) }
    var showEnergyInfo by remember { mutableStateOf(false) }
    var mealName by remember { mutableStateOf("") }
    var moodBefore by remember { mutableStateOf(1f) }
    var moodAfter by remember { mutableStateOf(1f) }
    var energyBefore by remember { mutableStateOf(1f) }
    var energyAfter by remember { mutableStateOf(1f) }
    val today = mealDataStoreManager.getTodayDate()

    val mealLog by mealDataStoreManager.getMealsForDate(today).collectAsState(initial = emptyList())

    val viewModel = remember { MealInsightViewModel(mealDataStoreManager) }
    val insights by viewModel.insightMessages.collectAsState()
    var showInsights by remember { mutableStateOf(false) }


    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFF3E0)) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Hi Raajas!",
                            color = Color(0xFF6D4C41),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Let's log your meal.",
                            color = Color(0xFF6D4C41),
                            fontSize = 14.sp
                        )
                    }

                    Box(modifier = Modifier.padding(end = 6.dp)) {
                        IconButton(
                            onClick = {
                                showInsights = true
                                viewModel.generateInsights() // Trigger here directly
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White, shape = RoundedCornerShape(18.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Insights",
                                tint = Color(0xFFFFEB3B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }


            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SelectorCard("Meal Type", selectedMealType, Icons.Default.LocalCafe) { showMealTypeDialog = true }
                        Spacer(modifier = Modifier.height(12.dp))
                        SelectorCard("Food Category", selectedFoodCategory, Icons.Default.Fastfood) { showFoodCategoryDialog = true }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = mealName,
                            onValueChange = { mealName = it },
                            label = { Text("Meal Name", color = Color(0xFFFF9800)) },
                            placeholder = { Text("Enter name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF9800),
                                unfocusedBorderColor = Color(0xFFFF9800)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OrangeSlider("Mood Before Eating", moodBefore, { moodBefore = it }) { showMoodInfo = true }
                        Spacer(modifier = Modifier.height(8.dp))
                        OrangeSlider("Mood After Eating", moodAfter, { moodAfter = it }) { showMoodInfo = true }
                        Spacer(modifier = Modifier.height(8.dp))
                        OrangeSlider("Energy Before Eating", energyBefore, { energyBefore = it }) { showEnergyInfo = true }
                        Spacer(modifier = Modifier.height(8.dp))
                        OrangeSlider("Energy After Eating", energyAfter, { energyAfter = it }) { showEnergyInfo = true }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val entry = MealEntry(today, mealDataStoreManager.getCurrentTime(), selectedMealType, mealName, selectedFoodCategory, moodBefore.toInt(), moodAfter.toInt(), energyBefore.toInt(), energyAfter.toInt())
                        scope.launch {
                            mealDataStoreManager.saveMeal(entry)
                            analytics.logEvent("meal_logged", Bundle().apply {
                                putString("meal_type", selectedMealType)
                                putString("category", selectedFoodCategory)
                            })
                            mealName = ""; moodBefore = 3f; moodAfter = 3f; energyBefore = 3f; energyAfter = 3f
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Log Meal", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D4C41))
                }
            }

            item {
                Spacer(modifier = Modifier.height(22.dp))
                Text("Today you had...",fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800) , modifier = Modifier.padding(vertical = 8.dp))
            }

            items(mealLog, key = { it.time + it.mealName }) { entry ->
                val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart) {
                        scope.launch { mealDataStoreManager.deleteMeal(entry) }
                        true
                    } else false
                })

                SwipeToDismissBox(state = dismissState, backgroundContent = {
                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                        Box(Modifier.fillMaxSize().background(Color.Red).padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) {
                            Text("REMOVE", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }) {
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(
                        0xFFFFCC80
                    )
                    )) {
                        Column(Modifier.padding(10.dp)) {
                            Text("🍽 ${entry.mealType}: ${entry.mealName}", style = MaterialTheme.typography.bodyMedium , color = Color(0xFF6D4C41))
                            Text("📅 ${entry.date} ➔ ${entry.time}", style = MaterialTheme.typography.bodySmall , color = Color(0xFF6D4C41))
                            Text("Category: ${entry.foodCategory}", style = MaterialTheme.typography.bodySmall , color = Color(0xFF6D4C41))
                            Text("Mood: ${entry.moodBefore} ➔ ${entry.moodAfter}", style = MaterialTheme.typography.bodySmall , color = Color(0xFF6D4C41))
                            Text("Energy: ${entry.energyBefore} ➔ ${entry.energyAfter}", style = MaterialTheme.typography.bodySmall , color = Color(0xFF6D4C41))
                        }
                    }
                }
            }


            if (showInsights) {
                item {
                    AlertDialog(
                        onDismissRequest = { showInsights = false },
                        confirmButton = {
                            TextButton(onClick = { showInsights = false }) { Text("Close") }
                        },
                        title = { Text("💡 Insights", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Log.d("InsightUI", "Insights: $insights")
                                if (insights.isEmpty()) {
                                    Text("Nothing to see here yet. Come back after a few meals!", color = Color.Gray)
                                } else {
                                    insights.forEach { msg ->
                                        Text("• $msg", modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
        if (showMoodInfo) {
            MoodInfoDialog { showMoodInfo = false }
        }

        if (showEnergyInfo) {
            EnergyInfoDialog { showEnergyInfo = false }
        }

    }
    if (showMealTypeDialog) {
        MealTypeDialog(
            onSelect = {
                selectedMealType = it
                showMealTypeDialog = false
            },
            onDismiss = { showMealTypeDialog = false }
        )
    }

    if (showFoodCategoryDialog) {
        FoodCategoryDialog(
            onSelect = {
                selectedFoodCategory = it
                showFoodCategoryDialog = false
            },
            onDismiss = { showFoodCategoryDialog = false }
        )
    }
}


@Composable
fun SelectorCard(label: String, selected: String, icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier
                    .size(24.dp)
                    .border(1.dp, Color(0xFFFF9800), RoundedCornerShape(6.dp))
                    .padding(4.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFFF9800))
            }
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(selected, color = Color(0xFFFF9800), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun OrangeSlider(label: String, value: Float, onValueChange: (Float) -> Unit, showInfoDialog: () -> Unit) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFFF3E0), Color(0xFFFFCC80))
    )
    var showTooltip by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6D4C41),
                modifier = Modifier.weight(1f)
            )
            PulsingInfoIcon(onClick = showInfoDialog)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 20.dp) // Top padding allows space for tooltip
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                val percentage = (value - 1f) / 4f
                if (showTooltip) {
                    Box(
                        modifier = Modifier
                            .offset(x = (percentage * 240).dp - 12.dp, y = (-28).dp)
                            .align(Alignment.TopStart)
                            .background(Color.White, shape = RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFFF9800), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("${value.toInt()}", color = Color(0xFF6D4C41), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Slider(
                    value = value,
                    onValueChange = {
                        onValueChange(it)
                        showTooltip = true
                    },
                    valueRange = 1f..5f,
                    steps = 3,
                    onValueChangeFinished = { showTooltip = false },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF9800),
                        activeTrackColor = Color(0xFFFF9800),
                        inactiveTrackColor = Color(0xFFFFCC80)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (showTooltip) {
                    val percentage = (value - 1f) / 4f
                    val alignment = Alignment.CenterStart
                    val offset = with(LocalDensity.current) {
                        val width = 240.dp.toPx()
                        val thumbOffset = (percentage * width).dp
                        Modifier.padding(start = thumbOffset)
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = (percentage * 240).dp - 12.dp, y = (-28).dp)
                            .align(Alignment.TopStart)
                            .background(Color.White, shape = RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFFF9800), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("${value.toInt()}", color = Color(0xFF6D4C41), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun IconSelectorGrid(
    title: String,
    options: List<Pair<String, ImageVector>>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFFCC80),
        title = { Text(title,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6D4C41),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .wrapContentWidth(Alignment.CenterHorizontally)) },
        confirmButton = {},
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.chunked(3).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowItems.forEach { (label, icon) ->
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFFF9800), RoundedCornerShape(12.dp))
                                    .clickable { onSelect(label) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(icon, contentDescription = label, tint = Color(0xFFFF9800), modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(label, fontSize = 10.sp, color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun PulsingInfoIcon(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )
    IconButton(onClick = onClick, modifier = Modifier.scale(scale)) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Info",
            tint = Color(0xFFFF9800),
            modifier = Modifier.size(22.dp)
        )
    }
}


@Composable
fun MealTypeDialog(onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val mealTypes = listOf(
        "Breakfast" to Icons.Default.LocalCafe,
        "Snack" to Icons.Default.LocalPizza,
        "Lunch" to Icons.Default.Restaurant,
        "Dinner" to Icons.Default.DinnerDining
    )
    IconSelectorGrid("SELECT MEAL TYPE", mealTypes, onSelect, onDismiss)
}

@Composable
fun FoodCategoryDialog(onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val categories = listOf(
        "Home-Cooked" to Icons.Default.Home,
        "Junk Snacks" to Icons.Default.LocalPizza,
        "Salads" to Icons.Default.Restaurant,
        "Desserts" to Icons.Default.Icecream,
        "Fast Food" to Icons.Default.Fastfood
    )
    IconSelectorGrid("SELECT CATEGORY", categories, onSelect, onDismiss)
}

@Composable
fun MoodInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mood Scale",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6D4C41),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .wrapContentWidth(Alignment.CenterHorizontally)) },
        text = {
            Column {
                MoodItem("5 - Blissful", "Excited, Delighted, Happy", Color(0xFFFFC107), "😄")
                MoodItem("4 - Relaxed", "Content, Serene, Pleasure", Color(0xFFFFE082), "😊")
                MoodItem("3 - Neutral", "Meh", Color(0xFFBCAAA4), "😐")
                MoodItem("2 - Sad", "Disappointed, Bored, Depressed", Color(0xFF90CAF9), "😞")
                MoodItem("1 - Angry", "Furious, Annoyed, Disgusted", Color(0xFFFF8A65), "😠")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        },
        containerColor = Color(0xFFFFF3E0)
    )
}

@Composable
fun MoodItem(title: String, description: String, color: Color, emoji: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF6D4C41))
            Text(description, fontSize = 12.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun EnergyInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Energy Scale",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6D4C41),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .wrapContentWidth(Alignment.CenterHorizontally)) },
        text = {
            Column {
                MoodItem("5 - Supercharged", "Energized, Focused, Active", Color(0xFF81C784), "⚡")
                MoodItem("4 - Alert", "Motivated, Fresh, Light", Color(0xFFA5D6A7), "🌟")
                MoodItem("3 - Neutral", "Normal, Stable", Color(0xFFBCAAA4), "🙂")
                MoodItem("2 - Tired", "Low, Slow, Yawning", Color(0xFF90CAF9), "😴")
                MoodItem("1 - Exhausted", "Lethargic, Sleepy, Drained", Color(0xFFE57373), "🥱")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        },
        containerColor = Color(0xFFFFF3E0)
    )
}







