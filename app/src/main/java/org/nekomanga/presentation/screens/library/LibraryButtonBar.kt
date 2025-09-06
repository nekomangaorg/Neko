package org.nekomanga.presentation.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.library.LibraryScreenActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import eu.kanade.tachiyomi.ui.library.filter.FilterUnread
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.icons.CollapseAllIcon
import org.nekomanga.presentation.components.icons.ExpandAllIcon
import org.nekomanga.presentation.theme.Size

@Composable
fun LibraryButtonBar(
    modifier: Modifier = Modifier,
    libraryScreenActions: LibraryScreenActions,
    libraryScreenState: State<LibraryScreenState>,
    showCollapseAll: Boolean,
    groupByClick: () -> Unit,
) {
    val filterScrollState = rememberScrollState()

    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(filterScrollState),
        horizontalArrangement = Arrangement.spacedBy(Size.small),
    ) {
        Gap(Size.small)

        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            AnimatedVisibility(showCollapseAll) {
                FilledTonalButton(
                    shapes =
                        ButtonShapes(
                            shape = ButtonGroupDefaults.connectedLeadingButtonShape,
                            pressedShape = ButtonGroupDefaults.connectedLeadingButtonPressShape,
                        ),
                    onClick = { libraryScreenActions.collapseExpandAllCategories() },
                ) {
                    Icon(
                        imageVector =
                            if (libraryScreenState.value.allCollapsed) ExpandAllIcon
                            else CollapseAllIcon,
                        contentDescription = null,
                    )
                }
            }

            FilledTonalButton(
                shapes =
                    ButtonShapes(
                        shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                        pressedShape = ButtonGroupDefaults.connectedTrailingButtonPressShape,
                    ),
                onClick = groupByClick,
            ) {
                Text(text = stringResource(R.string.group_library_by))
            }
        }
        AnimatedVisibility(libraryScreenState.value.hasActiveFilters) {
            FilledIconButton(onClick = libraryScreenActions.clearActiveFilters) {
                Icon(imageVector = Icons.Default.Clear, contentDescription = null)
            }
        }

        val unreadToggleList =
            listOf(
                ToggleButtonFields(
                    buttonChecked =
                        libraryScreenState.value.libraryFilters.filterUnread
                            is FilterUnread.NotStarted,
                    buttonOnCheckedChange = { enabling ->
                        libraryScreenActions.filterUnreadToggled(
                            if (enabling) FilterUnread.NotStarted else FilterUnread.Inactive
                        )
                    },
                    buttonText = stringResource(R.string.not_started),
                ),
                ToggleButtonFields(
                    buttonChecked =
                        libraryScreenState.value.libraryFilters.filterUnread
                            is FilterUnread.InProgress,
                    buttonOnCheckedChange = { enabling ->
                        libraryScreenActions.filterUnreadToggled(
                            if (enabling) FilterUnread.InProgress else FilterUnread.Inactive
                        )
                    },
                    buttonText = stringResource(R.string.in_progress),
                ),
                ToggleButtonFields(
                    buttonChecked =
                        libraryScreenState.value.libraryFilters.filterUnread is FilterUnread.Unread,
                    buttonOnCheckedChange = { enabling ->
                        libraryScreenActions.filterUnreadToggled(
                            if (enabling) FilterUnread.Unread else FilterUnread.Inactive
                        )
                    },
                    buttonText = stringResource(R.string.read),
                ),
                ToggleButtonFields(
                    buttonChecked =
                        libraryScreenState.value.libraryFilters.filterUnread is FilterUnread.Read,
                    buttonOnCheckedChange = { enabling ->
                        libraryScreenActions.filterUnreadToggled(
                            if (enabling) FilterUnread.Read else FilterUnread.Inactive
                        )
                    },
                    buttonText = stringResource(R.string.unread),
                ),
            )

        ConnectedToggleButtons(unreadToggleList)

        Gap(Size.small)
    }
}

@Composable
private fun ConnectedToggleButtons(buttons: List<ToggleButtonFields>) {
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        buttons.forEachIndexed { index, fields ->
            ToggleButton(
                checked = fields.buttonChecked,
                onCheckedChange = fields.buttonOnCheckedChange,
                shapes =
                    when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        buttons.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
            ) {
                Text(fields.buttonText)
            }
        }
    }
}

private data class ToggleButtonFields(
    val buttonChecked: Boolean,
    val buttonOnCheckedChange: (Boolean) -> Unit,
    val buttonText: String,
)
