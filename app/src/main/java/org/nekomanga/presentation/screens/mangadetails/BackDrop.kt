package org.nekomanga.presentation.screens.mangadetails

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import eu.kanade.tachiyomi.util.system.toMangaCacheKey
import org.nekomanga.domain.manga.Artwork

@Composable
fun BackDrop(artwork: Artwork, showBackdrop: Boolean, modifier: Modifier = Modifier, generatePalette: (drawable: Drawable) -> Unit = {}) {
    Box {
        if (showBackdrop && artwork.vibrantColor != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Color(artwork.vibrantColor).copy(alpha = .15f),
                    ),
            )
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artwork)
                .memoryCacheKey(artwork.mangaId.toMangaCacheKey())
                .allowHardware(false)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .alpha(.2f),

            onSuccess = { generatePalette(it.result.drawable) },
        )

    }
}
