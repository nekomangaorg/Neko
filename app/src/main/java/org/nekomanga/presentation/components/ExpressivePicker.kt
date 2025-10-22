package org.nekomanga.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.ui.theme.ThemePreviews
import org.nekomanga.ui.theme.ThemedPreviews

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> ExpressivePicker(
    value: T,
    items: List<T>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 5,
    itemHeight: Dp = 48.dp, // Fixed height for items
    selectedContainerHeight: Dp = 56.dp, // Fixed height for items
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {
    require(visibleItemsCount % 2 != 0) { "Visible items count must be an odd number" }

    // Check for an empty list
    if (items.isEmpty()) return

    // Find the initial index based on the current value. If not found, use the first item.
    val initialIndex = remember(value, items) { items.indexOf(value).coerceAtLeast(0) }
    val listStartIndex = initialIndex - visibleItemsCount / 2

    val listState =
        rememberLazyListState(initialFirstVisibleItemIndex = listStartIndex.coerceAtLeast(0))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val leadingSpacersCount = visibleItemsCount / 2

    // Internal state to track the value set by a user interaction (scroll/fling)
    var internalScrollValue by remember { mutableStateOf(value) }

    // 1. Update the value state when scrolling settles
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val fullListCenterIndex = listState.firstVisibleItemIndex + leadingSpacersCount
            val selectedItemIndexInList = fullListCenterIndex - leadingSpacersCount

            if (selectedItemIndexInList in items.indices) {
                val newValue = items[selectedItemIndexInList]
                // Update both the external and internal tracking value
                onValueChange(newValue)
                internalScrollValue = newValue
            }
        }
    }

    // 2. Center the picker only when the external 'value' changes AND it differs
    //    from the value set by the last internal scroll interaction.
    LaunchedEffect(value) {
        // Only run programmatic scroll if 'value' was changed externally
        if (value != internalScrollValue) {
            val targetIndex = items.indexOf(value)

            // Calculate the expected first visible index to center the target item
            val expectedFirstVisibleIndex = targetIndex - leadingSpacersCount

            if (
                targetIndex != -1 &&
                    listState.firstVisibleItemIndex != expectedFirstVisibleIndex &&
                    expectedFirstVisibleIndex >= 0
            ) {
                // Animate to the new external value
                listState.animateScrollToItem(expectedFirstVisibleIndex)

                // Update internalScrollValue after scroll to prevent scroll-back
                internalScrollValue = value
            }
        }
    }

    Box(
        modifier = modifier.fillMaxWidth().height(itemHeight * visibleItemsCount),
        contentAlignment = Alignment.Center,
    ) {
        // --- Number Scrolling List ---
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Top Spacers to center the first item visually
            items(visibleItemsCount / 2) { Spacer(Modifier.height(itemHeight)) }

            itemsIndexed(items = items, key = { index, item -> "$index-${item.hashCode()}" }) {
                index,
                item ->
                // Note: centerIndexTarget is calculated here based on current scroll state
                val centerIndexTarget = listState.firstVisibleItemIndex + (visibleItemsCount / 2)
                val distanceToCenter = abs(index - centerIndexTarget + (visibleItemsCount / 2))

                val isSelected = distanceToCenter == 0
                val alpha =
                    when (distanceToCenter) {
                        0 -> 1.0f
                        1 -> 0.6f
                        else -> 0.3f
                    }

                val style =
                    if (isSelected) {
                        MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.surface
                        )
                    } else {
                        MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                // Use a single Box to contain the item and handle the selection background
                Box(
                    modifier = Modifier.height(selectedContainerHeight).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        if (item is String) {
                            Box(
                                modifier =
                                    Modifier.height(selectedContainerHeight)
                                        .fillMaxWidth()
                                        .background(
                                            color = themeColorState.primaryColor.copy(alpha = 0.8f),
                                            shape = MaterialShapes.Gem.toShape(),
                                        )
                            )
                        } else {
                            Box(
                                modifier =
                                    Modifier.height(selectedContainerHeight)
                                        .width(selectedContainerHeight)
                                        .background(
                                            color = themeColorState.primaryColor.copy(alpha = 0.8f),
                                            shape = MaterialShapes.Gem.toShape(),
                                        )
                            )
                        }
                    }

                    // The Text content (using item.toString() for generic display)
                    Text(
                        text = item.toString(),
                        style = style,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().alpha(alpha),
                    )
                }
            }

            // Bottom Spacers to center the last item visually
            items(visibleItemsCount / 2) { Spacer(Modifier.height(itemHeight)) }
        }

        // --- Separator Lines (M3-style Box replacement for deprecated Divider) ---
        val lineThickness = 1.dp
        val lineYOffset = (itemHeight * (visibleItemsCount / 2))

        // Top Line
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(lineThickness)
                    .align(Alignment.TopCenter)
                    .padding(top = lineYOffset)
                    .background(themeColorState.containerColor.copy(alpha = 0.5f))
        )

        // Bottom Line
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(lineThickness)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = lineYOffset)
                    .background(themeColorState.containerColor.copy(alpha = 0.5f))
        )
    }
}

// -------------------------------------------------------------------------------------------------

@ThemePreviews
@Composable
private fun PreviewGenericPicker() {
    ThemedPreviews {
        // --- Preview 1: Integer Range ---
        val numberItems = remember { (1..30).toList() }
        var selectedNumber by remember { mutableStateOf(15) }

        Text("Integer Picker", style = MaterialTheme.typography.titleMedium)
        ExpressivePicker(
            value = selectedNumber,
            items = numberItems,
            onValueChange = { newValue -> selectedNumber = newValue },
            modifier = Modifier.width(150.dp).padding(bottom = 16.dp),
            visibleItemsCount = 5,
        )

        // --- Preview 2: String List ---
        val stringItems = remember {
            listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        }
        var selectedDay by remember { mutableStateOf("Wednesday") }

        Text("String Picker", style = MaterialTheme.typography.titleMedium)
        ExpressivePicker(
            value = selectedDay,
            items = stringItems,
            onValueChange = { newValue -> selectedDay = newValue },
            modifier = Modifier.width(150.dp),
            visibleItemsCount = 5,
        )
    }
}
