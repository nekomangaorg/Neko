package org.nekomanga.presentation.screens.manga

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.manga.MangaConstants.ChapterActions
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
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
            onClick = { chapterActions.open(chapterItem) },
            onBookmark = {
                chapterActions.mark(
                    listOf(chapterItem),
                    if (chapterItem.chapter.bookmark) ChapterMarkActions.UnBookmark(true)
                    else ChapterMarkActions.Bookmark(true),
                )
            },
            onRead = {
                chapterActions.mark(
                    listOf(chapterItem),
                    if (chapterItem.chapter.read) ChapterMarkActions.Unread(true)
                    else ChapterMarkActions.Read(true),
                )
            },
            onWebView = { chapterActions.openInBrowser(chapterItem) },
            onComment = { chapterActions.openComment(chapterItem.chapter.mangaDexChapterId) },
            onDownload = { downloadAction ->
                chapterActions.download(listOf(chapterItem), downloadAction)
            },
            markPrevious = { read -> chapterActions.markPrevious(chapterItem, read) },
            blockScanlator = { blockType, blocked ->
                chapterActions.blockScanlator(blockType, blocked)
            },
        )
    }
    if (listCardType != ListCardType.Bottom) {
        Spacer(modifier = Modifier.size(Size.tiny))
    }
}
