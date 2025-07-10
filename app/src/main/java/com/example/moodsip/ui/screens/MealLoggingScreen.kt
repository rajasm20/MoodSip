package com.example.moodsip.ui.screens

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.moodsip.data.MealDataStoreManager
import com.example.moodsip.data.MealEntry
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

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

    val mealLog = produceState<List<MealEntry>>(initialValue = emptyList(), key1 = today) {
        mealDataStoreManager.getMealsForDate(today).collect {
            value = it
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("Log a Meal", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(8.dp))

        Text("Meal Type")
        DropdownMenuBox(items = listOf("Breakfast", "Lunch", "Dinner", "Snack"), selected = selectedMealType) {
            selectedMealType = it
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Food Category")
        DropdownMenuBox(items = listOf("Home-Cooked", "Junk Snacks", "Salads and Healthy Bowls", "Desserts and Sweets", "Fast Food & Take Out"), selected = selectedFoodCategory) {
            selectedFoodCategory = it
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = mealName,
            onValueChange = { mealName = it },
            label = { Text("Meal Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Mood Before Eating: ${moodBefore.toInt()}")
        Slider(value = moodBefore, onValueChange = { moodBefore = it }, valueRange = 1f..5f, steps = 3)

        Text("Mood After Eating: ${moodAfter.toInt()}")
        Slider(value = moodAfter, onValueChange = { moodAfter = it }, valueRange = 1f..5f, steps = 3)

        Text("Energy Before Eating: ${energyBefore.toInt()}")
        Slider(value = energyBefore, onValueChange = { energyBefore = it }, valueRange = 1f..5f, steps = 3)

        Text("Energy After Eating: ${energyAfter.toInt()}")
        Slider(value = energyAfter, onValueChange = { energyAfter = it }, valueRange = 1f..5f, steps = 3)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
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
                moodBefore = 3f
                moodAfter = 3f
                energyBefore = 3f
                energyAfter = 3f
            }
        }) {
            Text("Log Meal")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Today's Meals", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(mealLog.value) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🍽 ${entry.mealType}: ${entry.mealName}", style = MaterialTheme.typography.bodyLarge)
                        Text("📅 ${entry.date} ⏰ ${entry.time}", style = MaterialTheme.typography.bodySmall)
                        Text("Category: ${entry.foodCategory}", style = MaterialTheme.typography.bodySmall)
                        Text("Mood: ${entry.moodBefore} ➡ ${entry.moodAfter}", style = MaterialTheme.typography.bodySmall)
                        Text("Energy: ${entry.energyBefore} ➡ ${entry.energyAfter}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun DropdownMenuBox(items: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Select") },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
