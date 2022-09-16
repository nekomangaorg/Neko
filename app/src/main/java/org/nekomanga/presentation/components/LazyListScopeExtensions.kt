package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

fun <T> LazyListScope.gridItems(
    items: List<T>,
    columns: Int,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    modifier: Modifier = Modifier,
    itemModifier: Modifier = Modifier,
    itemContent: @Composable BoxScope.(T) -> Unit,
) {
    val rows = if (items.isEmpty()) 0 else 1 + (items.count() - 1) / columns
    items(rows) { rowIndex ->
        Row(horizontalArrangement = horizontalArrangement, modifier = modifier) {
            for (columnIndex in 0 until columns) {
                val itemIndex = rowIndex * columns + columnIndex
                if (itemIndex < items.count()) {
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .then(itemModifier),
                        propagateMinConstraints = true,
                    ) {
                        itemContent.invoke(this, items[itemIndex])
                    }
                } else {
                    Spacer(Modifier.weight(1f, fill = true))
                }
            }
        }
    }
}
