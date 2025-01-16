package org.nekomanga.presentation.screens.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import jp.wasabeef.gap.Gap
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.components.DownloadButton
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.theme.Size

@Composable
fun UpdatesCard(
    modifier: Modifier = Modifier,
    chapterItem: ChapterItem,
    mangaTitle: String,
    artwork: Artwork,
    outlineCovers: Boolean,
    mangaClick: () -> Unit,
    chapterClick: (Long) -> Unit,
    downloadClick: (MangaConstants.DownloadAction) -> Unit,
) {
    UpdatesRow(
        modifier = modifier,
        chapterItem = chapterItem,
        mangaTitle = mangaTitle,
        artwork = artwork,
        outlineCovers = outlineCovers,
        mangaClick = mangaClick,
        chapterClick = chapterClick,
        downloadClick = downloadClick,
    )
}

@Composable
private fun UpdatesRow(
    modifier: Modifier = Modifier,
    chapterItem: ChapterItem,
    mangaTitle: String,
    artwork: Artwork,
    outlineCovers: Boolean,
    mangaClick: () -> Unit,
    chapterClick: (Long) -> Unit,
    downloadClick: (MangaConstants.DownloadAction) -> Unit,
) {
    val mediumAlphaColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { chapterClick(chapterItem.chapter.id) }
                .padding(vertical = Size.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Gap(Size.small)
        FeedCover(
            artwork = artwork,
            outlined = outlineCovers,
            coverSize = Size.extraHuge,
            onClick = mangaClick,
        )
        Column(modifier = Modifier.padding(horizontal = Size.small).weight(3f)) {
            val titleColor = getReadTextColor(isRead = chapterItem.chapter.read)
            val updatedColor = getReadTextColor(isRead = chapterItem.chapter.read, mediumAlphaColor)
            FeedChapterTitleLine(
                isBookmarked = chapterItem.chapter.bookmark,
                language = chapterItem.chapter.language,
                chapterNumber = chapterItem.chapter.chapterNumber,
                title = chapterItem.chapter.name,
                style = MaterialTheme.typography.bodyLarge,
                textColor = titleColor,
            )
            Text(
                text = "Updated ${chapterItem.chapter.dateUpload.timeSpanFromNow}",
                style = MaterialTheme.typography.labelSmall,
                color = updatedColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
            downloadState = chapterItem.downloadState,
            downloadProgress = chapterItem.downloadProgress,
            onDownload = downloadClick,
        )
    }
}
