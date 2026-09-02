package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.data.download.DownloadManager
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
import uy.kohesive.injekt.injectLazy

/** Headless implementation of [BaseViewer] for Webtoon continuous vertical reading mode. */
class WebtoonViewer(val activity: ReaderActivity, val noWebtoonTag: Boolean = false) : BaseViewer {

    val downloadManager: DownloadManager by injectLazy()

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
        scope.cancel()
    }

    private var isInitialLoad = true

    /** Tells this viewer to set the given [chapters] as active. */
    override fun setChapters(chapters: ViewerChapters) {
        TimberKt.d { "setChapters" }
        val forceTransition = config.alwaysShowChapterTransition
        val screenHeight = activity.resources.displayMetrics.heightPixels
        items = controller.buildItems(chapters, forceTransition, screenHeight)
        activity.updateWebtoonViewerItems()

        if (isInitialLoad) {
            isInitialLoad = false
            val pages = chapters.currChapter.pages ?: return
            val requestedIndex = min(chapters.currChapter.requestedPage, pages.lastIndex)
            if (requestedIndex in pages.indices) {
                moveToPage(pages[requestedIndex], false)
            }
        }
    }

    /** Tells this viewer to move to the given [page]. */
    override fun moveToPage(page: ReaderPage, animated: Boolean) {
        TimberKt.d { "moveToPage" }
        val position = controller.findPageIndex(items, page)
        if (position != -1) {
            requestedPagePosition = WebtoonPagePosition(position, animated)
        } else {
            TimberKt.d { "Page $page not found in items" }
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
