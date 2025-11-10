package org.nekomanga.presentation.components.scaffold

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.main.states.PullRefreshState
import org.nekomanga.presentation.components.PullRefresh

@Composable
fun ChildScreenScaffold(
    pullRefreshState: PullRefreshState = PullRefreshState(),
    drawUnderTopBar: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    PullRefresh(
        enabled = pullRefreshState.enabled,
        isRefreshing = pullRefreshState.isRefreshing,
        onRefresh = pullRefreshState.onRefresh,
        trackColor = pullRefreshState.trackColor ?: MaterialTheme.colorScheme.secondary,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = topBar,
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val padding =
                if (drawUnderTopBar) {
                    PaddingValues(
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateEndPadding(layoutDirection),
                        bottom = innerPadding.calculateBottomPadding(),
                        top = 0.dp,
                    )
                } else {
                    innerPadding
                }

            content(padding)
        }
    }
}
