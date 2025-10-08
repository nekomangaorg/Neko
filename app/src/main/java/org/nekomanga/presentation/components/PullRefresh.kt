package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
    val state = rememberPullToRefreshState()

    if (enabled) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            state = state,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = state,
                    isRefreshing = refreshing,
                    containerColor = backgroundColor,
                    color = contentColor,
                )
            },
        ) {
            content()
        }
    } else {
        Box { content() }
    }
}
