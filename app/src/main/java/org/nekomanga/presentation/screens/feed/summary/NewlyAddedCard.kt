package org.nekomanga.presentation.screens.feed.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import jp.wasabeef.gap.Gap
import me.saket.swipe.SwipeAction
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.components.ChapterSwipe
import org.nekomanga.presentation.components.DownloadButton
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.feed.FeedChapterTitleLine
import org.nekomanga.presentation.screens.feed.FeedCover
import org.nekomanga.presentation.screens.feed.getReadTextColor
import org.nekomanga.presentation.theme.Size

@Composable
fun NewlyAddedCard(
    modifier: Modifier = Modifier,
    chapterItem: ChapterItem,
    mangaTitle: String,
    dateAdded: Long,
    artwork: Artwork,
    outlineCovers: Boolean,
    mangaClick: () -> Unit,
    chapterClick: (Long) -> Unit,
    chapterSwipe: (ChapterItem) -> Unit,
    downloadClick: (MangaConstants.DownloadAction) -> Unit,
) {
    val swipeAction =
        SwipeAction(
            icon =
                rememberVectorPainter(
                    if (chapterItem.chapter.read) Icons.Default.VisibilityOff
                    else Icons.Default.Visibility
                ),
            background = MaterialTheme.colorScheme.secondary,
            onSwipe = { chapterSwipe(chapterItem) },
        )

    ChapterSwipe(
        modifier = modifier.padding(vertical = Dp(1f)),
        endSwipeActions = listOf(swipeAction),
    ) {
        NewlyAddedRow(
            modifier = modifier,
            chapterItem = chapterItem,
            mangaTitle = mangaTitle,
            dateAdded = dateAdded,
            artwork = artwork,
            outlineCovers = outlineCovers,
            mangaClick = mangaClick,
            chapterClick = chapterClick,
            downloadClick = downloadClick,
        )
    }
}

@Composable
private fun NewlyAddedRow(
    modifier: Modifier = Modifier,
    chapterItem: ChapterItem,
    mangaTitle: String,
    dateAdded: Long,
    artwork: Artwork,
    outlineCovers: Boolean,
    mangaClick: () -> Unit,
    chapterClick: (Long) -> Unit,
    downloadClick: (MangaConstants.DownloadAction) -> Unit,
) {
    val mediumAlphaColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.highAlphaLowContrast)

    val titleColor = getReadTextColor(isRead = chapterItem.chapter.read)
    val updatedColor = getReadTextColor(isRead = chapterItem.chapter.read, mediumAlphaColor)

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
            shoulderOverlayCover = chapterItem.chapter.read,
            onClick = mangaClick,
        )
        Column(modifier = Modifier.padding(horizontal = Size.small).weight(3f)) {
            Text(
                text = mangaTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FeedChapterTitleLine(
                isBookmarked = chapterItem.chapter.bookmark,
                language = chapterItem.chapter.language,
                title = chapterItem.chapter.name,
                style = MaterialTheme.typography.bodyMedium,
                textColor = updatedColor,
            )
            Text(
                text = "Added ${dateAdded.timeSpanFromNow}",
                style = MaterialTheme.typography.labelSmall,
                color = updatedColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DownloadButton(
            downloadState = chapterItem.downloadState,
            downloadProgress = chapterItem.downloadProgress,
            defaultDisableColor = chapterItem.chapter.read,
            onClick = downloadClick,
        )
    }
}
