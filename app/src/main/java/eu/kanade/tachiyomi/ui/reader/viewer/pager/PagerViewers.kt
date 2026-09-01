package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import kotlinx.coroutines.launch

/** Implementation of a left to right PagerViewer. */
class L2RPagerViewer(activity: ReaderActivity) : PagerViewer(activity) {
    /** Creates a new left to right pager. */
    override fun createPager(): Pager {
        return Pager(activity)
    }
}

/** Implementation of a right to left PagerViewer. */
class R2LPagerViewer(activity: ReaderActivity) : PagerViewer(activity) {
    /** Creates a new right to left pager. */
    override fun createPager(): Pager {
        return Pager(activity)
    }

    /** Moves to the next page. On a R2L pager the next page is the one at the left. */
    override fun moveToNext() {
        moveLeft()
    }

    /** Moves to the previous page. On a R2L pager the previous page is the one at the right. */
    override fun moveToPrevious() {
        moveRight()
    }

    override fun moveRight() {
        val current = currentPagePosition
        val item = adapter.joinedItems.getOrNull(current)
        val unwrapped = if (item is Pair<*, *>) item.first else item
        if (unwrapped is ChapterTransition.Prev && unwrapped.to != null) {
            activity.lifecycleScope.launch { activity.loadChapter(unwrapped.to.chapter) }
            return
        }
        if (current < adapter.count - 1) {
            hasMoved = true
            requestedPagePosition = (current + 1) to true
            val holder = (currentPage as? ReaderPage)?.let { getPageHolder(it) }
            if (holder != null && config.navigateToPan && holder.canPanRight()) {
                holder.panRight()
            } else {
                pager.setCurrentItem(current + 1, config.usePageTransitions)
            }
        }
    }

    override fun moveLeft() {
        val current = currentPagePosition
        val item = adapter.joinedItems.getOrNull(current)
        val unwrapped = if (item is Pair<*, *>) item.first else item
        if (unwrapped is ChapterTransition.Next && unwrapped.to != null) {
            activity.lifecycleScope.launch { activity.loadChapter(unwrapped.to.chapter) }
            return
        }
        if (current > 0) {
            hasMoved = true
            requestedPagePosition = (current - 1) to true
            val holder = (currentPage as? ReaderPage)?.let { getPageHolder(it) }
            if (holder != null && config.navigateToPan && holder.canPanLeft()) {
                holder.panLeft()
            } else {
                pager.setCurrentItem(current - 1, config.usePageTransitions)
            }
        }
    }
}

/** Implementation of a vertical (top to bottom) PagerViewer. */
class VerticalPagerViewer(activity: ReaderActivity) : PagerViewer(activity) {
    /** Creates a new vertical pager. */
    override fun createPager(): Pager {
        return Pager(activity, isHorizontal = false)
    }

    override fun moveToNext() {
        val current = currentPagePosition
        val item = adapter.joinedItems.getOrNull(current)
        val unwrapped = if (item is Pair<*, *>) item.first else item
        if (unwrapped is ChapterTransition.Next && unwrapped.to != null) {
            activity.lifecycleScope.launch { activity.loadChapter(unwrapped.to.chapter) }
            return
        }
        moveRight()
    }

    override fun moveToPrevious() {
        val current = currentPagePosition
        val item = adapter.joinedItems.getOrNull(current)
        val unwrapped = if (item is Pair<*, *>) item.first else item
        if (unwrapped is ChapterTransition.Prev && unwrapped.to != null) {
            activity.lifecycleScope.launch { activity.loadChapter(unwrapped.to.chapter) }
            return
        }
        moveLeft()
    }
}
