package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import me.saket.swipe.SwipeableActionsState
import me.saket.swipe.rememberSwipeableActionsState

@Composable
fun ChapterSwipe(
    modifier: Modifier = Modifier,
    startSwipeActions: List<SwipeAction> = emptyList<SwipeAction>(),
    endSwipeActions: List<SwipeAction> = emptyList<SwipeAction>(),
    content: @Composable BoxScope.() -> Unit,
) {

    val state: SwipeableActionsState = rememberSwipeableActionsState()
    SwipeableActionsBox(
        modifier = modifier,
        state = state,
        startActions = startSwipeActions,
        endActions = endSwipeActions,
        backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.secondary,
        content = content,
    )
}
