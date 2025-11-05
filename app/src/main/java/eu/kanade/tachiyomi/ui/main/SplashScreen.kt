package eu.kanade.tachiyomi.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlinx.coroutines.delay
import org.nekomanga.R
import org.nekomanga.ui.theme.ThemePreviews
import org.nekomanga.ui.theme.ThemedPreviews

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0f) }

    // Infinite transitions for logo animation
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by
        infiniteTransition.animateFloat(
            initialValue = -10f,
            targetValue = 10f,
            animationSpec =
                infiniteRepeatable(
                    tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
        )
    val pulse by
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec =
                infiniteRepeatable(
                    tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
        )
    val bounce by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -20f,
            animationSpec =
                infiniteRepeatable(
                    tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
        )

    val particleCount = 27
    val particles = remember {
        List(particleCount) {
            Particle(
                x = Random.nextFloat() * 800f,
                y = Random.nextFloat() * 1600f,
                radius = Random.nextFloat() * 4f + 2f,
                alpha = Random.nextFloat() * 0.5f + 0.3f,
            )
        }
    }

    // Animate logo scale in
    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
        delay(2000) // splash duration
        onSplashFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        // Draw floating particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                drawCircle(
                    color = Color.White.copy(alpha = p.alpha),
                    radius = p.radius,
                    center = androidx.compose.ui.geometry.Offset(p.x, p.y),
                )
            }
        }

        // Logo animation
        Image(
            painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "App Logo",
            modifier =
                Modifier.size(200.dp).graphicsLayer {
                    scaleX = scale.value * pulse
                    scaleY = scale.value * pulse
                    translationY = bounce
                    rotationZ = rotation
                    shadowElevation = 20f
                },
        )
    }
}

@ThemePreviews
@Composable
private fun SplashScreenPreview() {
    ThemedPreviews { SplashScreen {} }
}

data class Particle(var x: Float, var y: Float, var radius: Float, var alpha: Float)
