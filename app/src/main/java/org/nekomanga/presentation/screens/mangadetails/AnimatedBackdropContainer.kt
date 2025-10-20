package org.nekomanga.presentation.screens.mangadetails

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.components.theme.ThemeColorState

@Composable
fun AnimatedBackdropContainer(
    backdropHeight: Dp,
    themeColorState: ThemeColorState,
    artwork: Artwork,
    initialized: Boolean,
    showBackdrop: Boolean,
    generatePalette: (drawable: Drawable) -> Unit = {},
) {
    // 1. Define the target height based on your logic (e.g., showBackdrop)
    val targetHeight = if (showBackdrop) backdropHeight else 250.dp

    // 2. Create a transition based on the target height value
    val transition = updateTransition(targetHeight, label = "BackdropHeightTransition")

    // 3. Animate the height Dp value
    val animatedHeight by
        transition.animateDp(
            label = "BoxHeightAnimation",
            transitionSpec = {
                if (this.targetState < this.initialState) {
                    tween(durationMillis = 3000, delayMillis = 3000)
                } else {
                    tween(durationMillis = 1)
                }
            },
        ) { heightValue ->
            // This is the target value for the animation
            heightValue
        }

    // 4. Apply the animated height to the BackDrop modifier
    BackDrop(
        themeColorState = themeColorState,
        artwork = artwork,
        showBackdrop = showBackdrop,
        initialized = initialized,
        modifier =
            Modifier.fillMaxWidth()
                // Use the animatedHeight Dp value here
                .requiredHeightIn(min = 250.dp, max = maxOf(250.dp, animatedHeight)),
        generatePalette = generatePalette,
    )
}
