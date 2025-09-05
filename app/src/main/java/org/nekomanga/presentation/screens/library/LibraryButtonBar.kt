package org.nekomanga.presentation.screens.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.library.LibraryScreenActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
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
        Gap(Size.medium)

        FilledTonalIconButton(onClick = { libraryScreenActions.collapseExpandAllCategories() }) {
            Icon(
                imageVector =
                    if (libraryScreenState.value.allCollapsed) ExpandAllIcon else CollapseAllIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        FilledTonalButton(onClick = groupByClick) {
            Text(text = stringResource(R.string.group_library_by))
        }
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = true,
                onClick = {},
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text("Unread")
            }
            SegmentedButton(
                selected = false,
                onClick = {},
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text("Read")
            }
        }
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = true,
                onClick = {},
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text("Not started")
            }
            SegmentedButton(
                selected = false,
                onClick = {},
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text("In Progress")
            }
        }
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = true,
                onClick = {},
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text("Downloaded")
            }
            SegmentedButton(
                selected = false,
                onClick = {},
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text("Not downloaded")
            }
        }
        Gap(Size.medium)
    }
}
