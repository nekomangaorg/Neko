package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.theme.Size

@Composable
fun PullRefresh(
    enabled: Boolean = true,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    indicatorOffset: Dp = Size.none,
    backgroundColor: Color = MaterialTheme.colorScheme.secondary,
    contentColor: Color = MaterialTheme.colorScheme.onSecondary,
    content: @Composable () -> Unit,
) {
    val state =
        rememberPullRefreshState(
            refreshing = refreshing,
            onRefresh = onRefresh,
            refreshingOffset = indicatorOffset,
        )

    Box(Modifier.conditional(enabled) { this.pullRefresh(state, !refreshing) }) {
        content()

        Box(Modifier.padding().matchParentSize().clipToBounds()) {
            PullRefreshIndicator(
                refreshing = refreshing,
                state = state,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = backgroundColor,
                contentColor = contentColor,
            )
        }
    }
}
