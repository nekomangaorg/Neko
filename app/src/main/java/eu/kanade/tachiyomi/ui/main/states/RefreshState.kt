package eu.kanade.tachiyomi.ui.main.states

import androidx.compose.ui.graphics.Color

data class RefreshState(
    val enabled: Boolean = false,
    val isRefreshing: Boolean = false,
    val onRefresh: (() -> Unit)? = null,
    val trackColor: Color? = null,
)
