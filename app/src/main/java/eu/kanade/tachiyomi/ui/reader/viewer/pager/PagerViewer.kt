package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import kotlin.math.min
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.injectLazy

/** Headless implementation of [BaseViewer] for Pager reading modes (L2R, R2L, Vertical). */
abstract class PagerViewer(val activity: ReaderActivity) : BaseViewer {

    val downloadManager: DownloadManager by injectLazy()

    val scope = MainScope()

    /** Target page position to synchronize with Compose Pager. */
    var requestedPagePosition by mutableStateOf<Pair<Int, Boolean>?>(null)

    /** Configuration used by the pager. */
    val config = PagerConfig(scope, this)

    /** Pure domain controller for page pairing and transitions. */
    val controller = ReaderPagerController()

    /** Computed items list for Compose HorizontalPager. */
    var items by mutableStateOf<List<ReaderUiItem>>(emptyList())
        private set

    /** Currently active page index in the items list. */
    var currentPagePosition: Int = 0

    open val isRtl: Boolean
        get() = false

    val prevTransition: ChapterTransition.Prev?
        get() = controller.prevTransition

    val nextTransition: ChapterTransition.Next?
        get() = controller.nextTransition

    val currentChapter: ReaderChapter?
        get() = controller.currentChapter

    var hasMoved = false

    /** Holds forward position for reader activity shared transitions / landscape zoom. */
    var heldForwardZoom: Pair<Int, Boolean>? = null

    private var isTransitioning: Boolean = false

    init {
        config.imagePropertyChangedListener = {
            activity.isScrollingThroughPagesOrChapters = true
            activity.updatePagedViewerItems()
            activity.isScrollingThroughPagesOrChapters = false
        }
        config.reloadChapterListener = { activity.reloadChapters(it) }
        config.navigationModeChangedListener = {
            val showOnStart = config.navigationOverlayForNewUser
            activity.setNavigation(config.navigator, showOnStart)
        }
        config.navigationModeInvertedListener = { activity.showNavigationAgain() }
    }

    override fun destroy() {
        super.destroy()
        scope.cancel()
    }

    fun getShiftedPage(): ReaderPage? = controller.pageToShift

    fun updateShifting(page: ReaderPage? = null) {
        TimberKt.d { "update shifting" }
        controller.pageToShift =
            page ?: (items.getOrNull(currentPagePosition) as? ReaderUiItem.Page)?.page
    }

    fun triggerLoadChapter(chapter: Chapter) {
        if (isTransitioning) return
        isTransitioning = true
        activity.lifecycleScope.launch {
            try {
                activity.loadChapter(chapter)
            } finally {
                isTransitioning = false
            }
        }
    }

    /** Tells this viewer to set the given [chapters] as active. */
    override fun setChapters(chapters: ViewerChapters) {
        TimberKt.d { "setChapters" }
        val forceTransition = config.alwaysShowChapterTransition
        items =
            controller.buildItems(
                chapters = chapters,
                forceTransition = forceTransition,
                doublePages = config.doublePages,
                splitPages = config.splitPages,
                shiftDoublePage = config.shiftDoublePage,
                isRtl = isRtl,
            )
        activity.updatePagedViewerItems()

        val pages = chapters.currChapter.pages ?: return
        val requestedIndex = min(chapters.currChapter.requestedPage, pages.lastIndex)
        if (requestedIndex in pages.indices) {
            moveToPage(pages[requestedIndex], false)
        }
    }

    /** Tells this viewer to move to the given [page]. */
    override fun moveToPage(page: ReaderPage, animated: Boolean) {
        TimberKt.d { "moveToPage ${page.number}" }
        val position = controller.findPageIndex(items, page)
        if (position != -1) {
            currentPagePosition = position
            requestedPagePosition = position to animated
        } else {
            TimberKt.d { "Page $page not found in items" }
        }
    }

    override fun moveToNext() {
        moveRight()
    }

    override fun moveToPrevious() {
        moveLeft()
    }

    /** Moves to the page at the right. */
    open fun moveRight() {
        val current = requestedPagePosition?.first ?: currentPagePosition
        val item = items.getOrNull(current)
        if (
            item is ReaderUiItem.Transition &&
                item.transition is ChapterTransition.Next &&
                item.transition.to != null
        ) {
            triggerLoadChapter(item.transition.to.chapter)
            return
        }
        if (current < items.size - 1) {
            hasMoved = true
            val target = current + 1
            currentPagePosition = target
            requestedPagePosition = target to config.usePageTransitions
        }
    }

    /** Moves to the page at the left. */
    open fun moveLeft() {
        val current = requestedPagePosition?.first ?: currentPagePosition
        val item = items.getOrNull(current)
        if (
            item is ReaderUiItem.Transition &&
                item.transition is ChapterTransition.Prev &&
                item.transition.to != null
        ) {
            triggerLoadChapter(item.transition.to.chapter)
            return
        }
        if (current > 0) {
            hasMoved = true
            val target = current - 1
            currentPagePosition = target
            requestedPagePosition = target to config.usePageTransitions
        }
    }

    /** Moves to the page at the top (or previous). */
    protected open fun moveUp() {
        moveToPrevious()
    }

    /** Moves to the page at the bottom (or next). */
    protected open fun moveDown() {
        moveToNext()
    }

    /**
     * Called from the containing activity when a key [event] is received. It should return true if
     * the event was handled, false otherwise.
     */
    override fun handleKeyEvent(event: KeyEvent): Boolean {
        val isUp = event.action == KeyEvent.ACTION_UP
        val ctrlPressed = event.metaState.and(KeyEvent.META_CTRL_ON) > 0

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (!config.volumeKeysEnabled || activity.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) moveDown() else moveUp()
                }
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (!config.volumeKeysEnabled || activity.menuVisible) {
                    return false
                } else if (isUp) {
                    if (!config.volumeKeysInverted) moveUp() else moveDown()
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isUp) {
                    if (ctrlPressed) moveToNext() else moveRight()
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isUp) {
                    if (ctrlPressed) moveToPrevious() else moveLeft()
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> if (isUp) moveDown()
            KeyEvent.KEYCODE_DPAD_UP -> if (isUp) moveUp()
            KeyEvent.KEYCODE_PAGE_DOWN -> if (isUp) moveDown()
            KeyEvent.KEYCODE_PAGE_UP -> if (isUp) moveUp()
            KeyEvent.KEYCODE_MENU -> if (isUp) activity.toggleMenu()
            else -> return false
        }
        return true
    }

    /**
     * Called from the containing activity when a generic motion [event] is received. It should
     * return true if the event was handled, false otherwise.
     */
    override fun handleGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_CLASS_POINTER != 0) {
            when (event.action) {
                MotionEvent.ACTION_SCROLL -> {
                    if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
                        moveDown()
                    } else {
                        moveUp()
                    }
                    return true
                }
            }
        }
        return false
    }
}
