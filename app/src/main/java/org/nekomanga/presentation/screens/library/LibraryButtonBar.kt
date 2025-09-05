package org.nekomanga.presentation.screens.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonShapes
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
            FilledTonalIconButton(
                shapes =
                    IconButtonShapes(shape = CircleShape, pressedShape = RoundedCornerShape(25)),
                onClick = { libraryScreenActions.collapseExpandAllCategories() },
            ) {
                Icon(
                    imageVector =
                        if (libraryScreenState.value.allCollapsed) ExpandAllIcon
                        else CollapseAllIcon,
                    contentDescription = null,
                )
            }
            FilledTonalButton(shapes = ButtonDefaults.shapes(), onClick = groupByClick) {
                Text(text = stringResource(R.string.group_library_by))
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            ToggleButton(
                checked =
                    libraryScreenState.value.libraryFilters.filterUnread is FilterUnread.Enabled,
                onCheckedChange = { enabling ->
                    val action =
                        when (enabling) {
                            false -> FilterUnread.Inactive
                            true -> FilterUnread.Enabled
                        }
                    libraryScreenActions.filterUnreadToggled(action)
                },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
            ) {
                Text(stringResource(R.string.unread))
            }
            ToggleButton(
                checked =
                    libraryScreenState.value.libraryFilters.filterUnread is FilterUnread.Disabled,
                onCheckedChange = { enabling ->
                    val action =
                        when (enabling) {
                            false -> FilterUnread.Inactive
                            true -> FilterUnread.Disabled
                        }
                    libraryScreenActions.filterUnreadToggled(action)
                },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
            ) {
                Text(stringResource(R.string.read))
            }
        }

        Gap(Size.small)
    }
}
