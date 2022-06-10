package org.nekomanga.presentation.screens.mangadetails

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.image.coil.MangaCoverFetcher

@Composable
fun BackDrop(manga: Manga, buttonColor: Color, modifier: Modifier = Modifier, generatePalette: (drawable: Drawable) -> Unit = {}) {

    Box {
        if (buttonColor != MaterialTheme.colorScheme.primary && manga.vibrantCoverColor != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(manga.vibrantCoverColor!!).copy(alpha = .5f)),
            )
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(manga)
                .setParameter(MangaCoverFetcher.useCustomCover, false)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .alpha(.3f),
            onSuccess = { generatePalette(it.result.drawable) },
        )

    }
}
