package com.example.moodsip.ui.screens
import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodsip.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val tingle = remember { MediaPlayer.create(context, R.raw.tingle) }

    val scale = remember { Animatable(0.5f) }
    val rotation = remember { Animatable(0f) }
    val alphaText = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    var showLogo by remember { mutableStateOf(true) }

    val cursiveFont = FontFamily(Font(R.font.pacifico_regular))

    LaunchedEffect(Unit) {
        scale.animateTo(1.3f, tween(600, easing = FastOutSlowInEasing))
        rotation.animateTo(1440f, tween(2200, easing = LinearEasing))

        alphaText.animateTo(1f, tween(600))
        delay(300)

        tingle.start()
        delay(1500)
        tingle.release()

        screenAlpha.animateTo(0f, tween(600))

        showLogo = false
        onFinished()
    }

    if (showLogo) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFFBEF)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().alpha(screenAlpha.value)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(260.dp).clipToBounds()) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "MoodSip Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale.value,
                                    scaleY = scale.value,
                                    rotationY = rotation.value
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Sip. Log. Shine.",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6D4C41),
                        fontFamily = cursiveFont,
                        modifier = Modifier.alpha(alphaText.value)
                    )
                }
            }
        }
    }
}

