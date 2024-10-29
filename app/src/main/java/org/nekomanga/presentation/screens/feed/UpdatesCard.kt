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
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun UpdatesCard(
    chapterItem: ChapterItem,
    themeColorState: ThemeColorState,
    mangaTitle: String,
    artwork: Artwork,
    outlineCovers: Boolean,
    hideChapterTitles: Boolean,
    mangaClick: () -> Unit,
    chapterClick: (Long) -> Unit,
    downloadClick: (MangaConstants.DownloadAction) -> Unit,
) {
    UpdatesRow(
        chapterItem = chapterItem,
        themeColorState = themeColorState,
        mangaTitle = mangaTitle,
        artwork = artwork,
        outlineCovers = outlineCovers,
        hideChapterTitles = hideChapterTitles,
        mangaClick = mangaClick,
        chapterClick = chapterClick,
        downloadClick = downloadClick,
    )
}

@Composable
private fun UpdatesRow(
    chapterItem: ChapterItem,
    themeColorState: ThemeColorState,
    mangaTitle: String,
    artwork: Artwork,
    outlineCovers: Boolean,
    hideChapterTitles: Boolean,
    mangaClick: () -> Unit,
    chapterClick: (Long) -> Unit,
    downloadClick: (MangaConstants.DownloadAction) -> Unit,
) {
    val mediumAlphaColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)

    Row(
        modifier =
            Modifier.fillMaxWidth()
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
                hideChapterTitles = hideChapterTitles,
                isBookmarked = chapterItem.chapter.bookmark,
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
            themeColorState = themeColorState,
            downloadState = chapterItem.downloadState,
            downloadProgress = chapterItem.downloadProgress,
            onDownload = downloadClick,
        )
    }
}
