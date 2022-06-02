package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.image.coil.MangaCoverFetcher

@Composable
fun BackDrop(manga: Manga, shouldThemeBackdrop: Boolean = true, modifier: Modifier = Modifier) {
    Box {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(manga)
                .setParameter(MangaCoverFetcher.useCustomCover, false)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.alpha(.4f),
        )
        if (shouldThemeBackdrop) {
           
        }
    }
}
