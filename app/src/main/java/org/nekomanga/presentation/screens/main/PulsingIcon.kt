package org.nekomanga.presentation.screens.main

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun PulsingIcon(
    isPulsing: Boolean,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_tilt")

    // Scale animation (pulsing)
    val scale by
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (isPulsing) 1.2f else 1f,
            animationSpec =
                infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse),
            label = "pulse",
        )

    // Rotation animation (tilting)
    val rotation by
        infiniteTransition.animateFloat(
            initialValue = -10f, // Start tilted left
            targetValue = 10f, // End tilted right
            animationSpec =
                infiniteRepeatable(
                    animation = tween(300), // Make it a bit faster for a "wobble"
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "tilt",
        )

    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier =
            modifier
                .scale(scale)
                // Apply the rotation value only when isPulsing is true
                .rotate(if (isPulsing) rotation else 0f),
    )
}
