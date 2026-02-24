package org.nekomanga.presentation.screens.manga

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import coil3.asDrawable
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import eu.kanade.tachiyomi.data.coil.dynamicCover
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.components.theme.ThemeColorState

@Composable
fun BackDrop(
    themeColorState: ThemeColorState,
    artwork: Artwork,
    backdropOverlayAlpha: Float,
    dynamicCovers: Boolean,
    modifier: Modifier = Modifier,
    generatePalette: (drawable: Drawable) -> Unit = {},
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    println(
        "Artwork Check: ID=${artwork.mangaId}, Dynamic=${artwork.dynamicCover}, Default=${artwork.originalCover}"
    )

    val imageRequest =
        remember(artwork, dynamicCovers) {
            ImageRequest.Builder(context)
                .data(artwork)
                .dynamicCover(dynamicCovers)
                .allowHardware(false) // Necessary for palette generation
                .build()
        }

    Box(modifier = modifier) {
        if (backdropOverlayAlpha > 0f) {
            Box(
                modifier =
                    Modifier.matchParentSize()
                        .background(themeColorState.primaryColor.copy(alpha = .25f))
                        // Apply the animated alpha from the transition
                        .alpha(backdropOverlayAlpha)
            )
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.alpha(.2f),
            onSuccess = { generatePalette(it.result.image.asDrawable(resources)) },
        )
    }
}
