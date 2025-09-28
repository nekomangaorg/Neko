package org.nekomanga.presentation.screens.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import org.nekomanga.R
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.UiText

@Composable
fun LibraryAppBarActions(
    downloadChapters: (MangaConstants.DownloadAction) -> Unit,
    removeDownloads: (MangaConstants.DownloadAction) -> Unit,
    shareManga: () -> Unit,
    editCategoryClick: () -> Unit,
    markChapterClick: (ChapterMarkActions) -> Unit,
    removeFromLibraryClick: () -> Unit,
) {
    AppBarActions(
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
        actions =
            listOf(
                AppBar.Action(
                    title = UiText.StringResource(R.string.edit_categories),
                    icon = Icons.AutoMirrored.Outlined.Label,
                    onClick = editCategoryClick,
                ),
                AppBar.Action(
                    title = UiText.StringResource(R.string.remove_from_library),
                    icon = Icons.Outlined.DeleteOutline,
                    onClick = { removeFromLibraryClick() },
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
                                                downloadChapters(
                                                    MangaConstants.DownloadAction
                                                        .DownloadNextUnread(1)
                                                )
                                            },
                                        ),
                                        AppBar.OverflowAction(
                                            title = UiText.StringResource(R.string.next_5_unread),
                                            onClick = {
                                                downloadChapters(
                                                    MangaConstants.DownloadAction
                                                        .DownloadNextUnread(5)
                                                )
                                            },
                                        ),
                                        AppBar.OverflowAction(
                                            title = UiText.StringResource(R.string.next_10_unread),
                                            onClick = {
                                                downloadChapters(
                                                    MangaConstants.DownloadAction
                                                        .DownloadNextUnread(10)
                                                )
                                            },
                                        ),
                                    ),
                            ),
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.unread),
                                onClick = {
                                    downloadChapters(MangaConstants.DownloadAction.DownloadUnread)
                                },
                            ),
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.all),
                                onClick = {
                                    downloadChapters(MangaConstants.DownloadAction.DownloadAll)
                                },
                            ),
                        ),
                ),
                AppBar.OverflowAction(
                    title = UiText.StringResource(R.string.mark_as),
                    children =
                        listOf(
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.read),
                                onClick = { markChapterClick(ChapterMarkActions.Read()) },
                            ),
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.unread),
                                onClick = { markChapterClick(ChapterMarkActions.Unread()) },
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
                                    removeDownloads(MangaConstants.DownloadAction.RemoveAll)
                                },
                            ),
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.read),
                                onClick = {
                                    removeDownloads(MangaConstants.DownloadAction.RemoveRead)
                                },
                            ),
                        ),
                ),
                AppBar.OverflowAction(
                    title = UiText.StringResource(R.string.share),
                    onClick = shareManga,
                ),
                AppBar.OverflowAction(
                    title = UiText.StringResource(R.string.add_to_follows),
                    onClick = {},
                ),
            ),
    )
}
