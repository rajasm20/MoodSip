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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodsip.data.MealDataStoreManager
import com.example.moodsip.data.MealEntry
import com.example.moodsip.viewModel.MealInsightViewModel
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

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

    if (showInsights) viewModel.generateInsights()

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
                            onClick = { showInsights = true },
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
                        Spacer(modifier = Modifier.height(12.dp))
                        OrangeSlider("Mood Before Eating", moodBefore) { moodBefore = it }
                        Spacer(modifier = Modifier.height(12.dp))
                        OrangeSlider("Mood After Eating", moodAfter) { moodAfter = it }
                        Spacer(modifier = Modifier.height(12.dp))
                        OrangeSlider("Energy Before Eating", energyBefore) { energyBefore = it }
                        Spacer(modifier = Modifier.height(12.dp))
                        OrangeSlider("Energy After Eating", energyAfter) { energyAfter = it }
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("LOG MEAL", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            item {
                Text("Today's Meals", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp))
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
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE))) {
                        Column(Modifier.padding(10.dp)) {
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
                            TextButton(onClick = { showInsights = false }) { Text("Close") }
                        },
                        title = { Text("💡 Insights", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
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
fun OrangeSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6D4C41))
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
