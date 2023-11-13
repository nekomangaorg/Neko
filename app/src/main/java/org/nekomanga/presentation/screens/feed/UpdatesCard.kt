package org.nekomanga.presentation.screens.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.data.download.model.Download
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.components.DownloadButton
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.theme.Size

@Composable
fun UpdatesCard(firstChapter: SimpleChapter, buttonColor: Color, mangaTitle: String, artwork: Artwork, outlineCovers: Boolean, hideChapterTitles: Boolean, mangaClick: () -> Unit) {
    UpdatesRow(
        firstChapter = firstChapter,
        buttonColor = buttonColor,
        mangaTitle = mangaTitle,
        artwork = artwork,
        outlineCovers = outlineCovers,
        hideChapterTitles = hideChapterTitles,
        mangaClick = mangaClick,
    )
}

@Composable
private fun UpdatesRow(firstChapter: SimpleChapter, buttonColor: Color, mangaTitle: String, artwork: Artwork, outlineCovers: Boolean, hideChapterTitles: Boolean, mangaClick: () -> Unit) {
    val mediumAlphaColor = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { mangaClick() }
            .padding(vertical = Size.tiny),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        FeedCover(artwork = artwork, outlined = outlineCovers, coverSize = Size.extraHuge, onClick = mangaClick)
        Column(
            modifier = Modifier
                .padding(horizontal = Size.small)
                .weight(3f),
        ) {
            val titleColor = getReadTextColor(isRead = firstChapter.read)
            val updatedColor = getReadTextColor(isRead = firstChapter.read, mediumAlphaColor)
            FeedChapterTitleLine(
                hideChapterTitles = hideChapterTitles,
                isBookmarked = firstChapter.bookmark,
                chapterNumber = firstChapter.chapterNumber,
                title = firstChapter.name,
                style = MaterialTheme.typography.bodyLarge,
                textColor = titleColor,
            )
            Text(
                text = mangaTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = updatedColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DownloadButton(
            buttonColor,
            Download.State.NOT_DOWNLOADED,
            0f,
            Modifier
                .combinedClickable(
                    onClick = {
                        when (Download.State.NOT_DOWNLOADED) {
                            Download.State.NOT_DOWNLOADED -> Unit //onDownload(MangaConstants.DownloadAction.Download)
                            else -> Unit // chapterDropdown = true
                        }
                    },
                    onLongClick = {},
                ),
        )
    }
}
