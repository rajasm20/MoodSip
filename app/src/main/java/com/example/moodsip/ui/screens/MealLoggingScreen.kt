package com.example.moodsip.ui.screens

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodsip.data.MealDataStoreManager
import com.example.moodsip.data.MealEntry
import com.example.moodsip.viewModel.MealInsightViewModel
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealLoggerScreen(mealDataStoreManager: MealDataStoreManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val analytics = Firebase.analytics

    var selectedMealType by remember { mutableStateOf("Breakfast") }
    var selectedFoodCategory by remember { mutableStateOf("Home-Cooked") }
    var mealName by remember { mutableStateOf("") }
    var moodBefore by remember { mutableStateOf(3f) }
    var moodAfter by remember { mutableStateOf(3f) }
    var energyBefore by remember { mutableStateOf(3f) }
    var energyAfter by remember { mutableStateOf(3f) }
    val today = mealDataStoreManager.getTodayDate()

    val mealLog by mealDataStoreManager.getMealsForDate(today).collectAsState(initial = emptyList())

    val viewModel = remember { MealInsightViewModel(mealDataStoreManager) }
    var showInsights by remember { mutableStateOf(false) }

    LaunchedEffect(showInsights) {
        if (showInsights) viewModel.generateInsights()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFF9800)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "LOG A MEAL",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(8.dp)
                    )
                    IconButton(
                        onClick = { showInsights = true },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = "Insights", tint = Color.Yellow)
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OrangeLabeledDropdown(
                            label = "Meal Type",
                            items = listOf("Breakfast", "Lunch", "Dinner", "Snack"),
                            selected = selectedMealType,
                            onSelected = { selectedMealType = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OrangeLabeledDropdown(
                            label = "Food Category",
                            items = listOf(
                                "Home-Cooked", "Junk Snacks", "Salads and Healthy Bowls",
                                "Desserts and Sweets", "Fast Food & Take Out"
                            ),
                            selected = selectedFoodCategory,
                            onSelected = { selectedFoodCategory = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = mealName,
                            onValueChange = { mealName = it },
                            label = { Text("Meal Name") },
                            placeholder = { Text("Enter name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF9800),
                                unfocusedBorderColor = Color(0xFFFF9800)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OrangeSlider(label = "Mood Before Eating", value = moodBefore, onValueChange = { moodBefore = it })
                        OrangeSlider(label = "Mood After Eating", value = moodAfter, onValueChange = { moodAfter = it })
                        OrangeSlider(label = "Energy Before Eating", value = energyBefore, onValueChange = { energyBefore = it })
                        OrangeSlider(label = "Energy After Eating", value = energyAfter, onValueChange = { energyAfter = it })
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        val entry = MealEntry(
                            date = today,
                            time = mealDataStoreManager.getCurrentTime(),
                            mealType = selectedMealType,
                            mealName = mealName,
                            foodCategory = selectedFoodCategory,
                            moodBefore = moodBefore.toInt(),
                            moodAfter = moodAfter.toInt(),
                            energyBefore = energyBefore.toInt(),
                            energyAfter = energyAfter.toInt()
                        )
                        scope.launch {
                            mealDataStoreManager.saveMeal(entry)
                            analytics.logEvent("meal_logged", Bundle().apply {
                                putString("meal_type", selectedMealType)
                                putString("category", selectedFoodCategory)
                            })
                            mealName = ""
                            moodBefore = 3f; moodAfter = 3f; energyBefore = 3f; energyAfter = 3f
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Log Meal")
                }
            }
            item {
                Text("Today's Meals", style = MaterialTheme.typography.titleSmall)
            }
            items(mealLog, key = { it.time + it.mealName }) { entry ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            scope.launch { mealDataStoreManager.deleteMeal(entry) }
                            true
                        } else false
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Red)
                                    .padding(end = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text("REMOVE", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("🍽 ${entry.mealType}: ${entry.mealName}", style = MaterialTheme.typography.bodyMedium)
                            Text("📅 ${entry.date} ➔ ${entry.time}", style = MaterialTheme.typography.bodySmall)
                            Text("Category: ${entry.foodCategory}", style = MaterialTheme.typography.bodySmall)
                            Text("Mood: ${entry.moodBefore} ➔ ${entry.moodAfter}", style = MaterialTheme.typography.bodySmall)
                            Text("Energy: ${entry.energyBefore} ➔ ${entry.energyAfter}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (showInsights) {
                item {
                    AlertDialog(
                        onDismissRequest = { showInsights = false },
                        confirmButton = {
                            TextButton(onClick = { showInsights = false }) {
                                Text("Close")
                            }
                        },
                        title = { Text("💡 Insights", fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                viewModel.insightMessages.collectAsState().value.forEach { msg ->
                                    Text("• $msg", modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun OrangeSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6D4C41)
                )
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF9800),
                        activeTrackColor = Color(0xFFFF9800),
                        inactiveTrackColor = Color(0xFFFFCC80)
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}

@Composable
fun OrangeLabeledDropdown(
    label: String,
    items: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFF9800), shape = RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
            .clickable { expanded = true }
            .padding(8.dp)
    ) {
        Text(label, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
        Text(selected, modifier = Modifier.fillMaxWidth(), fontSize = 14.sp)
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.White)
                .fillMaxWidth()
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item.uppercase(),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Black
                        )
                    },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
                Divider()
            }
        }
    }
}
