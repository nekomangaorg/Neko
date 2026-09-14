package org.nekomanga.presentation.components.scaffold

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import eu.kanade.tachiyomi.ui.main.states.RefreshState
import org.nekomanga.presentation.components.PullRefresh
import org.nekomanga.presentation.extensions.clearFocusOnTap

@Composable
fun ChildScreenScaffold(
    refreshState: RefreshState = RefreshState(),
    scrollBehavior: TopAppBarScrollBehavior,
    snackbarHost: @Composable () -> Unit = {},
    topBar: @Composable () -> Unit,
    clearFocusOnTap: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    val clearFocusModifier = if (clearFocusOnTap) Modifier.clearFocusOnTap() else Modifier
    PullRefresh(
        enabled = refreshState.enabled,
        isRefreshing = refreshState.isRefreshing,
        onRefresh = refreshState.onRefresh,
        trackColor = refreshState.trackColor ?: MaterialTheme.colorScheme.secondary,
    ) {
        Scaffold(
            modifier =
                Modifier.fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .then(clearFocusModifier),
            topBar = topBar,
            snackbarHost = snackbarHost,
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}
