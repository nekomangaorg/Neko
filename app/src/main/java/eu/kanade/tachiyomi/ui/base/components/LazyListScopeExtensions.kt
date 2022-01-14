package eu.kanade.tachiyomi.ui.base.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import eu.kanade.tachiyomi.ui.base.components.theme.Shapes

fun <T> LazyListScope.gridItems(
    items: List<T>,
    columns: Int,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    modifier: Modifier = Modifier,
    itemContent: @Composable BoxScope.(T) -> Unit,
) {
    val rows = if (items.count() == 0) 0 else 1 + (items.count() - 1) / columns
    items(rows) { rowIndex ->
        Row(horizontalArrangement = horizontalArrangement, modifier = modifier) {
            for (columnIndex in 0 until columns) {
                val itemIndex = rowIndex * columns + columnIndex
                if (itemIndex < items.count()) {
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .background(color = Color.Transparent,
                                shape = RoundedCornerShape(Shapes.coverRadius)),
                        propagateMinConstraints = true
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