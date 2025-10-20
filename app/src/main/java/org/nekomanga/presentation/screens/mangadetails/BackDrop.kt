package org.nekomanga.presentation.screens.mangadetails

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import coil3.asDrawable
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.components.theme.ThemeColorState

@Composable
fun BackDrop(
    themeColorState: ThemeColorState,
    artwork: Artwork,
    initalized: Boolean,
    showBackdrop: Boolean,
    modifier: Modifier = Modifier,
    generatePalette: (drawable: Drawable) -> Unit = {},
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    val imageRequest =
        remember(artwork) {
            ImageRequest.Builder(context)
                .data(artwork)
                .allowHardware(false) // Necessary for palette generation
                .crossfade(500)
                .build()
        }

    Box {
        if (showBackdrop) {
            Box(
                modifier =
                    Modifier.matchParentSize()
                        .background(themeColorState.primaryColor.copy(alpha = .25f))
            )
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.alpha(.2f),
            onSuccess = { generatePalette(it.result.image.asDrawable(resources)) },
        )

        val alpha: Float by
            animateFloatAsState(
                targetValue = if (initalized) 1f else 0f,
                animationSpec = tween(durationMillis = 300),
            )

        Box(
            modifier =
                Modifier.align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(200.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface)
                        )
                    )
        )
    }
}
