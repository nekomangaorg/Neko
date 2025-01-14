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
    startSwipeAction: SwipeAction,
    endSwipeAction: SwipeAction,
    content: @Composable BoxScope.() -> Unit,
) {

    val state: SwipeableActionsState = rememberSwipeableActionsState()
    SwipeableActionsBox(
        modifier = modifier,
        state = state,
        startActions = listOf(startSwipeAction),
        endActions = listOf(endSwipeAction),
        backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.secondary,
        content = content,
    )
}
