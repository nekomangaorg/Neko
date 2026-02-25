package org.nekomanga.presentation.screens.manga

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.manga.MangaConstants.ChapterActions
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.presentation.components.ChapterRow
import org.nekomanga.presentation.components.listcard.ExpressiveListCard
import org.nekomanga.presentation.components.listcard.ListCardType
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun MangaChapterListItem(
    index: Int,
    chapterItem: ChapterItem,
    count: Int,
    themeColorState: ThemeColorState,
    shouldHideChapterTitles: Boolean,
    chapterActions: ChapterActions,
    onBookmark: (ChapterItem) -> Unit,
    onRead: (ChapterItem) -> Unit,
) {
    val listCardType =
        when {
            index == 0 && count > 1 -> ListCardType.Top
            index == count - 1 && count > 1 -> ListCardType.Bottom
            count == 1 -> ListCardType.Single
            else -> ListCardType.Center
        }

    ExpressiveListCard(
        modifier = Modifier.padding(horizontal = Size.small),
        listCardType = listCardType,
        themeColorState = themeColorState,
    ) {
        ChapterRow(
            themeColor = themeColorState,
            chapterItem = chapterItem,
            shouldHideChapterTitles = shouldHideChapterTitles,
            onClick = chapterActions.open,
            onBookmark = onBookmark,
            onRead = onRead,
            onWebView = chapterActions.openInBrowser,
            onComment = chapterActions.openComment,
            onDownload = chapterActions.download,
            markPrevious = chapterActions.markPrevious,
            blockScanlator = chapterActions.blockScanlator,
        )
    }
    if (listCardType != ListCardType.Bottom) {
        Spacer(modifier = Modifier.size(Size.tiny))
    }
}
