package com.example.moodsip
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.moodsip.data.DataStoreManager
import com.example.moodsip.network.WeatherService
import com.example.moodsip.ui.theme.HydrationAppTheme
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.launch
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

        setContent {
            HydrationAppTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                var glassCount by remember { mutableStateOf(0) }
                var hydrationGoal by remember { mutableStateOf(8) }
                val logList = remember { mutableStateListOf<String>() }
                val mediaPlayer = remember { MediaPlayer.create(context, R.raw.water_splash) }

                // Fetch hydrationGoal from Remote Config
                LaunchedEffect(true) {
                    val remoteConfig = Firebase.remoteConfig
                    remoteConfig.fetchAndActivate().addOnCompleteListener {
                        hydrationGoal = remoteConfig.getLong("hydration_goal").toInt()
                    }
                }

                // Load saved count
                LaunchedEffect(Unit) {
                    dataStore.getGlasses().collect {
                        glassCount = it
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

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (i in 0 until hydrationGoal) {
                                val isFilled = i < glassCount
                                Image(
                                    painter = painterResource(
                                        if (isFilled) R.drawable.glass_filled else R.drawable.glass_empty
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable {
                                            Toast.makeText(context, "Tapped!", Toast.LENGTH_SHORT).show()
                                            if (!isFilled) {
                                                glassCount++
                                                scope.launch { dataStore.saveGlasses(glassCount) }
                                                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                                logList.add("Drank at $timestamp")
                                                analytics.logEvent("glass_logged", null)
                                                mediaPlayer.start()
                                            } else {
                                                glassCount--
                                                scope.launch { dataStore.saveGlasses(glassCount) }
                                                logList.removeLastOrNull()
                                            }
                                        }
                                )
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
                                Text(logList[index])
                            }
                        }
                    }
                }
            }
        }
    }
}

