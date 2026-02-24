package org.nekomanga.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil3.ColorImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.ui.theme.Themed
import org.nekomanga.ui.theme.ThemedPreviews
import org.nekomanga.ui.theme.withThemes

@Composable
private fun MangaCoverPreviewContent(artwork: Artwork) {
    // Add a surface background to see the theme background color

    val previewHandler = AsyncImagePreviewHandler { ColorImage(Color.Red.toArgb()) }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(8.dp)) {
            Row {
                MangaCover.Book(artwork = artwork, dynamicCover = false)
                Spacer(modifier = Modifier.width(16.dp))
                MangaCover.Square(artwork = artwork, dynamicCover = false)
            }
        }
    }
}

@Preview
@Composable
private fun MangaCoverPreview(
    @PreviewParameter(ArtworkProvider::class) themedArtwork: Themed<Artwork>
) {
    ThemedPreviews(themedArtwork.themeConfig) { MangaCoverPreviewContent(themedArtwork.value) }
}

private class ArtworkProvider : PreviewParameterProvider<Themed<Artwork>> {
    override val values: Sequence<Themed<Artwork>> =
        sequenceOf(
                Artwork(cover = "dummy", mangaId = 1L, inLibrary = true, active = true),
                Artwork(cover = "", mangaId = 2L, inLibrary = false, active = false),
                Artwork(cover = "dummy", mangaId = 3L, inLibrary = true, active = false),
            )
            .withThemes()
}
