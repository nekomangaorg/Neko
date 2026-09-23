package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import org.nekomanga.domain.manga.MangaItem

/** Immutable UI configuration state for [ComposePagerViewer]. */
@Immutable
data class PagerViewerConfigUiModel(
    val initialIndex: Int = 0,
    val activeChapterId: Long? = null,
    val backgroundColor: Color = Color.Black,
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
    val doubleTapAnimDuration: Int = 300,
    val longTapEnabled: Boolean = true,
    val menuVisible: Boolean = false,
    val navigator: ViewerNavigation = DisabledNavigation(),
    val preloadPageAmount: Int = 4,
    val onToggleMenu: () -> Unit = {},
    val onNavigateAdjacent: (forward: Boolean) -> Unit = {},
    val onRetryTransition: (ReaderChapter) -> Unit = {},
    val onNavigateToChapter: ((Chapter, ChapterNavTarget) -> Unit)? = null,
    val onRequestPreloadChapter: ((ReaderChapter) -> Unit)? = null,
    val onPageLongTap: ((ReaderPage, ReaderPage?) -> Unit)? = null,
    @Deprecated(
        "Transition pages are now decoupled from DownloadManager and MangaItem via ChapterTransitionUiModel"
    )
    val manga: MangaItem? = null,
    @Deprecated(
        "Transition pages are now decoupled from DownloadManager and MangaItem via ChapterTransitionUiModel"
    )
    val downloadManager: DownloadManager? = null,
)
