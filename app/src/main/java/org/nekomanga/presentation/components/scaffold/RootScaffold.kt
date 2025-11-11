package org.nekomanga.presentation.components.scaffold

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import eu.kanade.tachiyomi.ui.main.states.RefreshState
import org.nekomanga.presentation.components.PullRefresh

@Composable
fun RootScaffold(
    refreshState: RefreshState = RefreshState(),
    scrollBehavior: TopAppBarScrollBehavior,
    mainSettingsExpanded: Boolean,
    navigationRail: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {

    PullRefresh(
        enabled = refreshState.enabled,
        isRefreshing = refreshState.isRefreshing,
        onRefresh = refreshState.onRefresh,
        blurBackground = mainSettingsExpanded,
        trackColor = refreshState.trackColor ?: MaterialTheme.colorScheme.secondary,
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            navigationRail() // if composable is empty this wont show

            Scaffold(
                modifier =
                    Modifier.fillMaxSize()
                        .weight(1f)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = topBar,
                bottomBar = bottomBar,
            ) { contentPadding ->
                content(contentPadding)
            }
        }
    }
}
