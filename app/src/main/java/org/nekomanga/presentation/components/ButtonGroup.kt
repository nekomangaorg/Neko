package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.nekomanga.presentation.extensions.surfaceColorAtElevationCustomColor
import org.nekomanga.presentation.theme.Size

@Composable
fun <T> ButtonGroup(
    items: List<T>,
    selectedItem: T,
    onItemClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        items.forEachIndexed { index, item ->
            val isSelected = item == selectedItem
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                onClick = { onItemClick(item) },
                selected = isSelected,
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        inactiveContainerColor =
                            MaterialTheme.colorScheme.surfaceColorAtElevationCustomColor(
                                MaterialTheme.colorScheme.primary,
                                Size.small,
                            ),
                        inactiveContentColor = MaterialTheme.colorScheme.primary,
                        activeBorderColor = MaterialTheme.colorScheme.primary,
                        inactiveBorderColor =
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = NekoColors.veryLowContrast
                            ),
                    ),
                modifier = Modifier.defaultMinSize(minHeight = Size.large),
                icon = {},
            ) {
                content(item)
            }
        }
    }
}
