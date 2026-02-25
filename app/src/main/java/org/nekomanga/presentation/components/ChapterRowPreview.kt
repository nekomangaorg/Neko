package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.online.utils.MdLang
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.ui.theme.Themed
import org.nekomanga.ui.theme.ThemedPreviews
import org.nekomanga.ui.theme.withThemes

@Composable
private fun ChapterRowPreviewContent(chapterItem: ChapterItem) {
    val themeColorState = defaultThemeColorState()
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(16.dp)) {
        ChapterRow(
            themeColor = themeColorState,
            chapterItem = chapterItem,
            onClick = {},
            onBookmark = {},
            onRead = {},
            onWebView = {},
            onComment = {},
            onDownload = { _, _ -> },
            blockScanlator = { _, _ -> },
            markPrevious = { _, _ -> },
        )
    }
}

@Preview
@Composable
private fun ChapterRowPreview(
    @PreviewParameter(ChapterItemProvider::class) themedChapterItem: Themed<ChapterItem>
) {
    ThemedPreviews(themedChapterItem.themeConfig) {
        ChapterRowPreviewContent(themedChapterItem.value)
    }
}

private class ChapterItemProvider : PreviewParameterProvider<Themed<ChapterItem>> {
    override val values: Sequence<Themed<ChapterItem>> =
        sequenceOf(
                // 1. Normal, Unread, English
                ChapterItem(
                    chapter =
                        SimpleChapter.create()
                            .copy(
                                name = "Chapter 1: The Beginning",
                                chapterNumber = 1f,
                                language = MdLang.ENGLISH.lang,
                                scanlator = "ScanGroup A",
                                dateUpload = System.currentTimeMillis(),
                            )
                ),
                // 2. Read, Bookmarked, Japanese
                ChapterItem(
                    chapter =
                        SimpleChapter.create()
                            .copy(
                                name = "Chapter 2: The Middle",
                                chapterNumber = 2f,
                                read = true,
                                bookmark = true,
                                language = MdLang.JAPANESE.lang,
                                scanlator = "ScanGroup B",
                                dateUpload = System.currentTimeMillis() - 86400000,
                            )
                ),
                // 3. Downloading, Progress 45%
                ChapterItem(
                    chapter =
                        SimpleChapter.create()
                            .copy(
                                name = "Chapter 3: The Climax",
                                chapterNumber = 3f,
                                language = MdLang.ENGLISH.lang,
                                scanlator = "ScanGroup A",
                                dateUpload = System.currentTimeMillis() - 172800000,
                            ),
                    downloadState = Download.State.DOWNLOADING,
                    downloadProgress = 45,
                ),
                // 4. Unavailable/Locked
                ChapterItem(
                    chapter =
                        SimpleChapter.create()
                            .copy(
                                name = "Chapter 4: The End",
                                chapterNumber = 4f,
                                language = MdLang.ENGLISH.lang,
                                isUnavailable = true,
                                scanlator = "Official Publisher",
                            )
                ),
            )
            .withThemes()
}
