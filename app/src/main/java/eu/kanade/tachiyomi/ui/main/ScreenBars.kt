package eu.kanade.tachiyomi.ui.main

import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

data class ScreenBars(
    val topBar: (@Composable () -> Unit)? = null,
    val scrollBehavior: TopAppBarScrollBehavior? = null,
)

val LocalBarUpdater =
    compositionLocalOf<(ScreenBars) -> Unit> {
        // Default empty implementation
        {}
    }
