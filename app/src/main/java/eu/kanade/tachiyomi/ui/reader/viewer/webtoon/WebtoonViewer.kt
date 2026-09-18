package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.data.coil.ReaderPageSplitFetcher
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import kotlin.math.min
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.nekomanga.logging.TimberKt

/**
 * Legacy implementation of [BaseViewer] for Webtoon continuous vertical reading mode.
 *
 * @deprecated Superceded by headless [ReaderPreloadEngine], [BuildWebtoonItemsUseCase], and
 *   stateless [ComposeWebtoonViewer].
 */
@Deprecated(
    "Use ReaderPreloadEngine and stateless ComposeWebtoonViewer with WebtoonViewerConfigUiModel instead"
)
class WebtoonViewer(val activity: ReaderActivity, val noWebtoonTag: Boolean = false) : BaseViewer {

    val scope = MainScope()

    data class WebtoonPagePosition(
        val targetPage: Int,
        val animated: Boolean,
        val timestamp: Long = System.nanoTime(),
    )

    /** Target page position to synchronize with Compose LazyList. */
    var requestedPagePosition by mutableStateOf<WebtoonPagePosition?>(null)

    /** Delta scroll to synchronize with Compose LazyList. */
    var requestedScrollDelta by mutableStateOf<Int?>(null)

    val hasMargins: Boolean
        get() = noWebtoonTag && !config.disableGaps

    /** Controller used for pure domain item building and split page calculations. */
    val controller = ReaderWebtoonController()

    /** Currently computed reader UI items. */
    var items by mutableStateOf<List<ReaderUiItem>>(emptyList())
        private set

    val prevTransition: ChapterTransition.Prev?
        get() = controller.prevTransition

    val nextTransition: ChapterTransition.Next?
        get() = controller.nextTransition

    val currentChapter: ReaderChapter?
        get() = controller.currentChapter

    /** Distance to scroll when the user taps on one side of the viewer. */
    private var scrollDistance = activity.resources.displayMetrics.heightPixels * 3 / 4

    /** Configuration used by this viewer. */
    val config = WebtoonConfig(scope)

    /** Headless preload engine for disk prefetching and memory cache warming. */
    val preloadEngine =
        ReaderPreloadEngine(
            context = activity,
            scope = scope,
            onPageSplit = { originalPage, insertPages -> splitPage(originalPage, insertPages) },
            isSplitTallPagesEnabled = { config.splitTallPages },
            getScreenHeight = { activity.resources.displayMetrics.heightPixels },
        )

    init {
        config.reloadViewerListener = { activity.viewModel.reloadViewer() }
        config.navigationModeChangedListener = {
            val showOnStart = config.navigationOverlayForNewUser
            activity.setNavigation(config.navigator, showOnStart)
        }
        config.navigationModeInvertedListener = { activity.showNavigationAgain() }
    }

    /** Destroys this viewer. Called when leaving the reader or swapping viewers. */
    override fun destroy() {
        super.destroy()
        preloadEngine.clear()
        scope.cancel()
        pendingPageMove = null
        ReaderPageSplitFetcher.clearCache()
    }

    private var activeChapterId: Long? = null
    private var isInitialLoad = true
    private var pendingPageMove: Pair<ReaderPage, Boolean>? = null

    /** Tells this viewer to set the given [chapters] as active. */
    override fun setChapters(chapters: ViewerChapters) {
        TimberKt.d { "setChapters" }
        val forceTransition = config.alwaysShowChapterTransition
        val screenHeight = activity.resources.displayMetrics.heightPixels
        val newItems =
            controller.buildItems(
                chapters = chapters,
                forceTransition = forceTransition,
                screenHeight = if (config.splitTallPages) screenHeight else 0,
                existingItems = items,
            )
        val chapterChanged = activeChapterId != chapters.currChapter.chapter.id
        activeChapterId = chapters.currChapter.chapter.id

        items = newItems
        activity.updateWebtoonViewerItems()

        val pages = chapters.currChapter.pages
        val requestedIndex = pages?.let { min(chapters.currChapter.requestedPage, it.lastIndex) }
        val targetPage =
            if (requestedIndex != null && requestedIndex in pages.indices) pages[requestedIndex]
            else pages?.firstOrNull()
        val initialActiveIndex =
            targetPage?.let { controller.findPageIndex(newItems, it) }?.takeIf { it != -1 } ?: 0
        preloadEngine.updateActiveIndex(initialActiveIndex, newItems, config.preloadPageAmount)

        val pending = pendingPageMove
        pendingPageMove = null
        if (
            pending != null && pending.first.chapter.chapter.id == chapters.currChapter.chapter.id
        ) {
            moveToPage(pending.first, pending.second)
        } else if (isInitialLoad) {
            isInitialLoad = false
            if (requestedIndex != null && requestedIndex in pages.indices) {
                moveToPage(pages[requestedIndex], false)
            }
        }
    }

    fun updateActiveIndex(activeIndex: Int) {
        preloadEngine.updateActiveIndex(activeIndex, items, config.preloadPageAmount)
    }

    /** Tells this viewer to move to the given [page]. */
    override fun moveToPage(page: ReaderPage, animated: Boolean) {
        TimberKt.d { "moveToPage for page ${page.number} in chapter ${page.chapter.chapter.id}" }
        if (activeChapterId != null && page.chapter.chapter.id != activeChapterId) {
            TimberKt.d {
                "Queuing moveToPage for non-active chapter ${page.chapter.chapter.id} (active is $activeChapterId)"
            }
            pendingPageMove = page to animated
            return
        }
        val position = controller.findPageIndex(items, page)
        if (position != -1) {
            pendingPageMove = null
            requestedPagePosition = WebtoonPagePosition(position, animated)
        } else {
            TimberKt.d { "Page $page not found in items, queuing" }
            pendingPageMove = page to animated
        }
    }

    /** Notifies the viewer that a tall page was split into [insertPages]. */
    fun splitPage(originalPage: ReaderPage, insertPages: List<ReaderPageSplit>) {
        items = controller.splitPage(items, originalPage, insertPages)
        activity.updateWebtoonViewerItems()
    }

    /** Scrolls up by [scrollDistance]. */
    override fun moveToPrevious() {
        requestedScrollDelta = -scrollDistance
    }

    /** Scrolls down by [scrollDistance]. */
    override fun moveToNext() {
        requestedScrollDelta = scrollDistance
    }

    /**
     * Called from the containing activity when a key [event] is received. It should return true if
     * the event was handled, false otherwise.
     */
    override fun handleKeyEvent(event: KeyEvent): Boolean {
        val isUp = event.action == KeyEvent.ACTION_UP

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (!config.volumeKeysEnabled || activity.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) moveToNext() else moveToPrevious()
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (!config.volumeKeysEnabled || activity.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) moveToPrevious() else moveToNext()
                }
            }
            KeyEvent.KEYCODE_MENU -> if (isUp) activity.toggleMenu()
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_PAGE_UP -> if (isUp) moveToPrevious()
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_PAGE_DOWN -> if (isUp) moveToNext()
            else -> return false
        }
        return true
    }

    /**
     * Called from the containing activity when a generic motion [event] is received. It should
     * return true if the event was handled, false otherwise.
     */
    override fun handleGenericMotionEvent(event: MotionEvent): Boolean {
        return false
    }
}
