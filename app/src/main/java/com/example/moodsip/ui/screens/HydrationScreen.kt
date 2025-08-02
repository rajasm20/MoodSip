
package com.example.moodsip.ui.screens
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodsip.R
import com.example.moodsip.data.DataStoreManager
import com.example.moodsip.util.NotificationHelper
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import androidx.compose.ui.text.style.TextAlign
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
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
    var showLogDialog by remember { mutableStateOf(false) }
    var glassCount by remember { mutableStateOf(0) }
    var hydrationGoal by remember { mutableStateOf(8) }
    val logList = remember { mutableStateListOf<String>() }
    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.water_splash) }
    var streakCount by remember { mutableStateOf(0) }
    var lastLogDate by remember { mutableStateOf("") }

    val analytics = Firebase.analytics
    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Load state
    LaunchedEffect(Unit) {
        dataStore.getGlasses().collect { glassCount = it }
    }
    LaunchedEffect(true) {
        dataStore.getLogEntriesForToday().collect { entries ->
            logList.clear()
            logList.addAll(entries.map { "Drank at $it" })
        }
    }

    //Temperature-based goal logic
    LaunchedEffect(temperature) {
        temperature?.let {
            val baseGoal = 8
            val tempAdjustment = ((it - 25) / 5).toInt().coerceAtLeast(0)
            hydrationGoal = baseGoal + tempAdjustment
            dataStore.saveDailyGoal(todayDate, hydrationGoal)

            if (hydrationGoal >= 10) {
                NotificationHelper.showHotWeatherNotification(context)
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFFFFF)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            //hydration header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hi Raajas 👋",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = Color(0xFF01579B)
                )
                temperature?.let {
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🌤 ${it.toInt()}°C",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold,fontSize = 18.sp),
                            color = Color(0xFF0277BD)
                        )
                    }
                }
            }

            // status hydration
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0288D1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                val hoursLeft = 24 - Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text("💧", fontSize = 18.sp)
                        Text(
                            text = "$glassCount / $hydrationGoal",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "glasses drunk",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }

                    //status divider
                    Box(
                        modifier = Modifier
                            .height(90.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.5f))
                    )


                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text("⏳", fontSize = 18.sp)
                        Text(
                            text = "$hoursLeft hours",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "left today",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }
            }





            // glass counting from here
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (i in 0 until hydrationGoal) {
                    val isFilled = i < glassCount
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clickable {
                                val now = Calendar.getInstance()
                                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                val currentHour = now.get(Calendar.HOUR_OF_DAY)
                                val timeOfDay = when (currentHour) {
                                    in 5..11 -> "morning"
                                    in 12..17 -> "afternoon"
                                    else -> "evening"
                                }
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                                if (!isFilled) {
                                    glassCount++
                                    scope.launch {
                                        dataStore.saveGlasses(glassCount)
                                        dataStore.saveDailyGlasses(today, glassCount)
                                        dataStore.saveLogEntry(today, timestamp)
                                    }
                                    logList.add("Drank at $timestamp")
                                    mediaPlayer.start()
                                    /*
                                    Point to note is the notifications are indeed smart just for the demo purposes,
                                    i have implemented them here rule based as well otherwise its handled by the workmanager
                                    Refer NotificationHelper.kt for deep dive
                                    */
                                    when (glassCount) {
                                        1 -> NotificationHelper.showFirstGlassNotification(context)// rule based just for demo purposes
                                        hydrationGoal / 2 -> NotificationHelper.showHalfwayNotification(context)// rule based just for demo purposes
                                        hydrationGoal - 1 -> NotificationHelper.showAlmostThereNotification(context)// rule based just for demo purposes
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
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = if (isFilled) "-" else if (i == glassCount) "+" else "",
                            color = if (isFilled) Color.Red else Color.Blue,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = glassCount / hydrationGoal.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = Color(0xFF0288D1)
            )

            CircularLottieHydrationProgress(
                glassCount = glassCount,
                goal = hydrationGoal,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )





            //Logstarting
            Button(
                onClick = { showLogDialog = true },
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
            ) {
                Text("View Logs", color = Color.White, fontWeight = FontWeight.Medium)
            }
            if (showLogDialog) {
                AlertDialog(
                    onDismissRequest = { showLogDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showLogDialog = false }) {
                            Text("Close", fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                        }
                    },

                    title = { Text("Hydration Log",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF01579B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally)) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (logList.isEmpty()) {
                                Text(
                                    text = "No entries yet!",
                                    fontSize = 16.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            } else {
                                logList.forEach { entry ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Text("💧", fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = entry.replace(Regex("(\\d{2}:\\d{2}):\\d{2}"), "$1"),
                                                fontSize = 16.sp,
                                                color = Color(0xFF0277BD),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.White,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                )
            }


        }
    }
}

@Composable
fun CelebrationOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE)), // light blue
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 32.dp, horizontal = 24.dp)
            ) {
                Text(
                    text = "🎉",
                    fontSize = 44.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Goal Reached!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF01579B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You drank all your glasses today!",
                    fontSize = 16.sp,
                    color = Color(0xFF0277BD),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0288D1),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Awesome!", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
fun CircularLottieHydrationProgress(
    glassCount: Int,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.circle_fill))

    val maxAnimationProgress = when (goal) {
        9 -> 0.75f
        10 -> 0.7f
        else -> 0.65f
    }

    val logicalProgress = (glassCount / goal.toFloat()).coerceIn(0f, 1f)
    val mappedProgress = (logicalProgress * maxAnimationProgress).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = mappedProgress,
        animationSpec = tween(durationMillis = 600)
    )

    val percentage = (logicalProgress * 100).toInt()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(300.dp)
    ) {
        LottieAnimation(
            composition = composition,
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize()
        )

        Text(
            text = "$percentage%",
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = Color(0xFF01579B)
        )
    }
}














