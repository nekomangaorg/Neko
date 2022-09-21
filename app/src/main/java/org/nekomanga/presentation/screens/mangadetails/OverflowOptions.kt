package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions

@Composable
fun OverflowOptions(chapterActions: MangaConstants.ChapterActions, chaptersProvider: () -> ImmutableList<ChapterItem>) {
    AppBarActions(
        actions = listOf(
            AppBar.OverflowAction(
                title = stringResource(R.string.download),
                children = listOf(
                    AppBar.OverflowAction(
                        title = stringResource(id = R.string.next_unread),
                        children = listOf(
                            AppBar.OverflowAction(
                                title = stringResource(id = R.string.next_1_unread),
                                onClick = { chapterActions.download(emptyList(), MangaConstants.DownloadAction.DownloadNextUnread(1)) },
                            ),
                            AppBar.OverflowAction(
                                title = stringResource(id = R.string.next_5_unread),
                                onClick = { chapterActions.download(emptyList(), MangaConstants.DownloadAction.DownloadNextUnread(5)) },

                            ),
                            AppBar.OverflowAction(
                                title = stringResource(id = R.string.next_10_unread),
                                onClick = { chapterActions.download(emptyList(), MangaConstants.DownloadAction.DownloadNextUnread(10)) },

                            ),
                        ),
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(id = R.string.unread),
                        onClick = { chapterActions.download(emptyList(), MangaConstants.DownloadAction.DownloadUnread) },
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(id = R.string.all),
                        onClick = { chapterActions.download(emptyList(), MangaConstants.DownloadAction.DownloadAll) },
                    ),
                ),
            ),
            AppBar.OverflowAction(
                title = stringResource(R.string.mark_all_as),
                children = listOf(
                    AppBar.OverflowAction(
                        title = stringResource(id = R.string.read),
                        onClick = { chapterActions.mark(chaptersProvider(), MangaConstants.MarkAction.Read(true)) },
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(id = R.string.unread),
                        onClick = { chapterActions.mark(chaptersProvider(), MangaConstants.MarkAction.Unread(true)) },
                    ),
                ),
            ),
            AppBar.OverflowAction(
                title = stringResource(R.string.remove_downloads),
                children = listOf(
                    AppBar.OverflowAction(
                        title = stringResource(id = R.string.all),
                        onClick = { chapterActions.download(emptyList(), MangaConstants.DownloadAction.RemoveAll) },
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(id = R.string.read),
                        onClick = { chapterActions.download(emptyList(), MangaConstants.DownloadAction.RemoveRead) },
                    ),
                ),
            ),
        ),
    )
}
