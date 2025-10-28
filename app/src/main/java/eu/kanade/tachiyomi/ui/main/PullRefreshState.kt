package eu.kanade.tachiyomi.ui.main

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import java.util.UUID

data class PullRefreshState(
    val id: UUID = UUID.randomUUID(),
    val enabled: Boolean = false,
    val isRefreshing: Boolean = false,
    val onRefresh: (() -> Unit)? = null,
    val trackColor: Color? = null,
)

val LocalPullRefreshState =
    compositionLocalOf<(PullRefreshState) -> Unit> {
        // Default empty implementation
        {}
    }
