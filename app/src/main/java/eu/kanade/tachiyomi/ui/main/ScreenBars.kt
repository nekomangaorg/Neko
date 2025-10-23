package eu.kanade.tachiyomi.ui.main

import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import java.util.UUID

data class ScreenBars(
    val topBar: (@Composable () -> Unit)? = null,
    val scrollBehavior: TopAppBarScrollBehavior? = null,
    val id: UUID = UUID.randomUUID(),
)

val LocalBarUpdater =
    compositionLocalOf<(ScreenBars) -> Unit> {
        // Default empty implementation
        {}
    }
