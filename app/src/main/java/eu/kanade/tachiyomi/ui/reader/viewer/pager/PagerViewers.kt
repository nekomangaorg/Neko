package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

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
        val current = requestedPagePosition?.first ?: currentPagePosition
        val item = adapter.joinedItems.getOrNull(current)
        val unwrapped = if (item is Pair<*, *>) item.first else item
        if (unwrapped is ChapterTransition.Prev && unwrapped.to != null) {
            triggerLoadChapter(unwrapped.to.chapter)
            return
        }
        if (current < adapter.count - 1) {
            hasMoved = true
            val target = current + 1
            requestedPagePosition = target to true
            val holder = (currentPage as? ReaderPage)?.let { getPageHolder(it) }
            if (holder != null && config.navigateToPan && holder.canPanRight()) {
                holder.panRight()
            } else {
                pager.setCurrentItem(target, config.usePageTransitions)
            }
        }
    }

    override fun moveLeft() {
        val current = requestedPagePosition?.first ?: currentPagePosition
        val item = adapter.joinedItems.getOrNull(current)
        val unwrapped = if (item is Pair<*, *>) item.first else item
        if (unwrapped is ChapterTransition.Next && unwrapped.to != null) {
            triggerLoadChapter(unwrapped.to.chapter)
            return
        }
        if (current > 0) {
            hasMoved = true
            val target = current - 1
            requestedPagePosition = target to true
            val holder = (currentPage as? ReaderPage)?.let { getPageHolder(it) }
            if (holder != null && config.navigateToPan && holder.canPanLeft()) {
                holder.panLeft()
            } else {
                pager.setCurrentItem(target, config.usePageTransitions)
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
}
