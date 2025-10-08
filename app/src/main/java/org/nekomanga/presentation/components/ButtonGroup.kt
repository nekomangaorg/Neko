package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.nekomanga.ui.theme.ThemePreviews
import org.nekomanga.ui.theme.ThemedPreviews

@Composable
fun <T> ButtonGroup(
    items: List<T>,
    selectedItem: T,
    onItemClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(T) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = item == selectedItem

            ToggleButton(
                checked = isSelected,
                onCheckedChange = { _ -> onItemClick(item) },
                colors =
                    ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                shapes =
                    when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        items.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
            ) {
                content(item)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ButtonGroupPreview() {
    ThemedPreviews {
        // Define the items for the button group
        val timePeriods = listOf("Summary", "History", "Updates")

        // State to track the currently selected item
        var selectedPeriod by remember { mutableStateOf(timePeriods.first()) }

        ButtonGroup(
            items = timePeriods,
            selectedItem = selectedPeriod,
            onItemClick = { item -> selectedPeriod = item },
            modifier = Modifier.fillMaxWidth(0.8f), // Constrain width for better visualization
        ) { item ->
            // The content for each button: a Text composable
            Text(text = item)
        }
    }
}
