package org.nekomanga.presentation.screens.manga

import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import kotlinx.collections.immutable.PersistentList
import org.nekomanga.R
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.theme.ThemeColorState

@Composable
fun MangaDetailsAppBarActions(
    chapterActions: MangaConstants.ChapterActions,
    themeColorState: ThemeColorState,
    chapters: PersistentList<ChapterItem>,
) {
    AppBarActions(
        themeColorState = themeColorState,
        actions =
            listOf(
                AppBar.OverflowAction(
                    title = UiText.StringResource(R.string.create_manga_folder),
                    onClick = { chapterActions.createMangaFolder() },
                ),
                AppBar.OverflowAction(
                    title = UiText.StringResource(R.string.download),
                    children =
                        listOf(
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.next_unread),
                                children =
                                    listOf(
                                        AppBar.OverflowAction(
                                            title = UiText.StringResource(R.string.next_1_unread),
                                            onClick = {
                                                chapterActions.download(
                                                    emptyList(),
                                                    MangaConstants.DownloadAction
                                                        .DownloadNextUnread(1),
                                                )
                                            },
                                        ),
                                        AppBar.OverflowAction(
                                            title = UiText.StringResource(R.string.next_5_unread),
                                            onClick = {
                                                chapterActions.download(
                                                    emptyList(),
                                                    MangaConstants.DownloadAction
                                                        .DownloadNextUnread(5),
                                                )
                                            },
                                        ),
                                        AppBar.OverflowAction(
                                            title = UiText.StringResource(R.string.next_10_unread),
                                            onClick = {
                                                chapterActions.download(
                                                    emptyList(),
                                                    MangaConstants.DownloadAction
                                                        .DownloadNextUnread(10),
                                                )
                                            },
                                        ),
                                    ),
                            ),
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.unread),
                                onClick = {
                                    chapterActions.download(
                                        emptyList(),
                                        MangaConstants.DownloadAction.DownloadUnread,
                                    )
                                },
                            ),
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.all),
                                onClick = {
                                    chapterActions.download(
                                        emptyList(),
                                        MangaConstants.DownloadAction.DownloadAll,
                                    )
                                },
                            ),
                        ),
                ),
                AppBar.OverflowAction(
                    title = UiText.StringResource(R.string.mark_all_as),
                    children =
                        listOf(
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.read),
                                onClick = {
                                    chapterActions.mark(chapters, ChapterMarkActions.Read(true))
                                },
                            ),
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.unread),
                                onClick = {
                                    chapterActions.mark(chapters, ChapterMarkActions.Unread(true))
                                },
                            ),
                        ),
                ),
                AppBar.OverflowAction(
                    title = UiText.StringResource(R.string.remove_downloads),
                    children =
                        listOf(
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.all),
                                onClick = {
                                    chapterActions.download(
                                        emptyList(),
                                        MangaConstants.DownloadAction.RemoveAll,
                                    )
                                },
                            ),
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.read),
                                onClick = {
                                    chapterActions.download(
                                        emptyList(),
                                        MangaConstants.DownloadAction.RemoveRead,
                                    )
                                },
                            ),
                        ),
                ),
            ),
    )
}
