package org.nekomanga.presentation.screens.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.runtime.Composable
import org.nekomanga.R
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.UiText

@Composable
fun LibraryAppBarActions(editCategoryClick: () -> Unit, removeFromLibraryClick: () -> Unit) {
    AppBarActions(
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
                                            onClick = {},
                                        ),
                                        AppBar.OverflowAction(
                                            title = UiText.StringResource(R.string.next_5_unread),
                                            onClick = {},
                                        ),
                                        AppBar.OverflowAction(
                                            title = UiText.StringResource(R.string.next_10_unread),
                                            onClick = {},
                                        ),
                                    ),
                            ),
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.unread),
                                onClick = {},
                            ),
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.all),
                                onClick = {},
                            ),
                        ),
                ),
                AppBar.OverflowAction(
                    title = UiText.StringResource(R.string.mark_as),
                    children =
                        listOf(
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.read),
                                onClick = {},
                            ),
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.unread),
                                onClick = {},
                            ),
                        ),
                ),
                AppBar.OverflowAction(
                    title = UiText.StringResource(R.string.remove_downloads),
                    children =
                        listOf(
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.all),
                                onClick = {},
                            ),
                            AppBar.OverflowAction(
                                title = UiText.StringResource(R.string.read),
                                onClick = {},
                            ),
                        ),
                ),
                AppBar.OverflowAction(title = UiText.StringResource(R.string.share), onClick = {}),
                AppBar.OverflowAction(
                    title = UiText.StringResource(R.string.add_to_follows),
                    onClick = {},
                ),
            )
    )
}
