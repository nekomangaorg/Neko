package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerPanDelegate

/** Immutable UI configuration state for [ComposePagerViewer]. */
@Immutable
data class PagerViewerConfigUiModel(
    val initialIndex: Int = 0,
    val activeChapterId: Long? = null,
    val backgroundColor: Color = Color.Black,
    val colorFilter: ColorFilter? = null,
    val isRtl: Boolean = false,
    val isVertical: Boolean = false,
    val animatedTransitions: Boolean = true,
    val imageScaleType: Int = 0,
    val pageLayout: PageLayout = PageLayout.SINGLE_PAGE,
    val doublePages: Boolean = false,
    val shiftDoublePage: Boolean = false,
    val invertDoublePages: Boolean = false,
    val doublePageGap: Int = 0,
    val zoomDoublePageSpreads: Boolean = false,
    val doublePageRotate: Boolean = false,
    val doublePageRotateReverse: Boolean = false,
    val zoomStart: Int = 0,
    val landscapeZoom: Boolean = false,
    val navigateToPan: Boolean = false,
    val doubleTapAnimDuration: Int = 300,
    val longTapEnabled: Boolean = true,
    val menuVisible: Boolean = false,
    val cropBorders: Boolean = false,
    val navigator: ViewerNavigation = DisabledNavigation(),
    val preloadPageAmount: Int = 4,
    val onToggleMenu: () -> Unit = {},
    val onNavigateAdjacent: (forward: Boolean) -> Unit = {},
    val onActivePanDelegateChanged: ((delegate: PagerPanDelegate?, active: Boolean) -> Unit)? =
        null,
    val onRetryTransition: (ReaderChapter) -> Unit = {},
    val onNavigateToChapter: ((Chapter, ChapterNavTarget) -> Unit)? = null,
    val onRequestPreloadChapter: ((ReaderChapter) -> Unit)? = null,
    val onPageLongTap: ((ReaderPage, ReaderPage?) -> Unit)? = null,
    val onWidePageDetected: ((ReaderPage) -> Unit)? = null,
)
