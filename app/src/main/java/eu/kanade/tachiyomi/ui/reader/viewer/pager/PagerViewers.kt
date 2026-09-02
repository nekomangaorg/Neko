package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem

/** Implementation of a left to right PagerViewer. */
class L2RPagerViewer(activity: ReaderActivity) : PagerViewer(activity)

/** Implementation of a right to left PagerViewer. */
class R2LPagerViewer(activity: ReaderActivity) : PagerViewer(activity) {
    override val isRtl: Boolean
        get() = true

    override fun moveToNext() {
        moveLeft()
    }

    override fun moveToPrevious() {
        moveRight()
    }

    override fun moveRight() {
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
        if (current < items.size - 1) {
            hasMoved = true
            val target = current + 1
            currentPagePosition = target
            requestedPagePosition = target to config.usePageTransitions
        }
    }

    override fun moveLeft() {
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
        if (current > 0) {
            hasMoved = true
            val target = current - 1
            currentPagePosition = target
            requestedPagePosition = target to config.usePageTransitions
        }
    }
}

/** Implementation of a vertical (top to bottom) PagerViewer. */
class VerticalPagerViewer(activity: ReaderActivity) : PagerViewer(activity)
