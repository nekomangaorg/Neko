package eu.kanade.tachiyomi.ui.main.states

import androidx.compose.ui.graphics.Color

data class PullRefreshState(
    val enabled: Boolean = false,
    val isRefreshing: Boolean = false,
    val onRefresh: (() -> Unit)? = null,
    val trackColor: Color? = null,
)
