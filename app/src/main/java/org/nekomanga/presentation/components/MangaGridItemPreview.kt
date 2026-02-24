package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.ui.theme.Themed
import org.nekomanga.ui.theme.ThemedPreviews
import org.nekomanga.ui.theme.withThemes

@Preview(widthDp = 400, showBackground = true)
@Composable
private fun MangaGridItemPreview(
    @PreviewParameter(DisplayMangaProvider::class) themedDisplayManga: Themed<DisplayManga>
) {
    val previewHandler = AsyncImagePreviewHandler { ColorImage(Color.Red.toArgb()) }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        ThemedPreviews(themedDisplayManga.themeConfig) {
            val displayManga = themedDisplayManga.value
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Comfortable Item
                MangaGridItem(
                    displayManga = displayManga,
                    shouldOutlineCover = true,
                    isComfortable = true,
                    showUnreadBadge = true,
                    unreadCount = 12,
                    showDownloadBadge = true,
                    downloadCount = 5,
                )

                // Compact Item
                MangaGridItem(
                    displayManga = displayManga,
                    shouldOutlineCover = true,
                    isComfortable = false,
                    showUnreadBadge = false,
                    unreadCount = 0,
                    showDownloadBadge = false,
                    downloadCount = 0,
                )
            }
        }
    }
}

private class DisplayMangaProvider : PreviewParameterProvider<Themed<DisplayManga>> {
    override val values: Sequence<Themed<DisplayManga>> =
        sequenceOf(
                DisplayManga(
                    mangaId = 1L,
                    inLibrary = true,
                    currentArtwork = Artwork(mangaId = 1L, inLibrary = true),
                    url = "",
                    originalTitle = "One Piece",
                    userTitle = "One Piece",
                    displayText = "Eiichiro Oda",
                ),
                DisplayManga(
                    mangaId = 2L,
                    inLibrary = false,
                    currentArtwork = Artwork(mangaId = 2L, inLibrary = false),
                    url = "",
                    originalTitle =
                        "Detailed Long Title: The Adventure of a Lifetime in Another World",
                    userTitle = "",
                    displayText = "Author Name • Artist Name",
                ),
                DisplayManga(
                    mangaId = 3L,
                    inLibrary = true,
                    currentArtwork = Artwork(mangaId = 3L, inLibrary = true),
                    url = "",
                    originalTitle = "No Subtitle",
                    userTitle = "No Subtitle",
                    displayText = "",
                ),
            )
            .withThemes()
}
