package org.nekomanga.presentation.screens.mangadetails

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun BackDrop(themeColorState: ThemeColorState, artworkProvider: () -> Artwork, showBackdropProvider: () -> Boolean, modifier: Modifier = Modifier, generatePalette: (drawable: Drawable) -> Unit = {}) {
    Box {
        if (showBackdropProvider()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        themeColorState.buttonColor.copy(alpha = .25f),
                    ),
            )
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkProvider())
                .allowHardware(false)
                .crossfade(true)
                .crossfade(500)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .alpha(.2f),

            onSuccess = {
                generatePalette(it.result.drawable)
            },
        )
    }
}
