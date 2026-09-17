package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import org.nekomanga.presentation.theme.Size

/** Immutable UI configuration state for [ComposeWebtoonViewer]. */
@Immutable
data class WebtoonViewerConfigUiModel(
    val initialIndex: Int = 0,
    val activeChapterId: Long? = null,
    val backgroundColor: Color = Color.Transparent,
    val contentPadding: PaddingValues = PaddingValues(bottom = Size.none),
    val sidePadding: Dp = Size.none,
    val sidePaddingPercent: Float = 0f,
    val hasGaps: Boolean = true,
    val enableZoomOut: Boolean = false,
    val animatedTransitions: Boolean = true,
    val doubleTapAnimDuration: Int = 300,
    val longTapEnabled: Boolean = true,
    val menuVisible: Boolean = false,
    val navigator: ViewerNavigation,
    val onToggleMenu: () -> Unit = {},
    val onRetryTransition: (ReaderChapter) -> Unit = {},
)
