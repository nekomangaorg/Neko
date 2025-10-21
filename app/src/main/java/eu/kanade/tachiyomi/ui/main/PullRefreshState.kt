package eu.kanade.tachiyomi.ui.main

import androidx.compose.runtime.compositionLocalOf

data class PullRefreshState(
    val enabled: Boolean = true,
    val isRefreshing: Boolean = false,
    val onRefresh: () -> Unit = {},
)

val LocalPullRefreshState =
    compositionLocalOf<(PullRefreshState) -> Unit> {
        // Default empty implementation
        {}
    }
