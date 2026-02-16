package org.nekomanga.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.ui.theme.ThemePreviews
import org.nekomanga.ui.theme.ThemedPreviews

@Composable
private fun MangaCoverPreviewContent(artwork: Artwork) {
    // Add a surface background to see the theme background color
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(8.dp)) {
        Column {
            Row {
                MangaCover.Book(artwork = artwork)
                Spacer(modifier = Modifier.width(16.dp))
                MangaCover.Square(artwork = artwork)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun MangaCoverPreview(@PreviewParameter(ArtworkProvider::class) artwork: Artwork) {
    ThemedPreviews { MangaCoverPreviewContent(artwork) }
}

private class ArtworkProvider : PreviewParameterProvider<Artwork> {
    override val values: Sequence<Artwork> =
        sequenceOf(
            Artwork(
                url = "https://example.com/cover.jpg",
                mangaId = 1L,
                inLibrary = true,
                active = true,
            ),
            Artwork(url = "", mangaId = 2L, inLibrary = false, active = false),
            Artwork(
                url = "https://example.com/cover2.jpg",
                mangaId = 3L,
                inLibrary = true,
                active = false,
            ),
        )
}
