package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem

/** Implementation of a left to right PagerViewer. */
@Deprecated("Use ComposePagerViewer with ReaderViewModel and ReaderNavCommand instead")
class L2RPagerViewer(activity: ReaderActivity) : PagerViewer(activity)

/** Implementation of a right to left PagerViewer. */
@Deprecated("Use ComposePagerViewer with ReaderViewModel and ReaderNavCommand instead")
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
        if (config.navigateToPan && canPanRight()) {
            panRight()
            return
        }
        val current =
            (targetPagePosition ?: requestedPagePosition?.first ?: currentPagePosition).coerceIn(
                0,
                (items.size - 1).coerceAtLeast(0),
            )
        val item = items.getOrNull(current)
        if (
            item is ReaderUiItem.Transition &&
                item.transition is ChapterTransition.Prev &&
                item.transition.to != null
        ) {
            targetPagePosition = null
            triggerLoadChapter(item.transition.to.chapter, navTarget = ChapterNavTarget.End)
            return
        }
        if (current < items.size - 1) {
            hasMoved = true
            val target = current + 1
            targetPagePosition = target
            currentPagePosition = target
            requestedPagePosition = target to config.usePageTransitions
        } else if (item !is ReaderUiItem.Transition) {
            targetPagePosition = null
            activity.viewModel.navigateAdjacentChapter(forward = false)
        }
    }

    override fun moveLeft() {
        if (config.navigateToPan && canPanLeft()) {
            panLeft()
            return
        }
        val current =
            (targetPagePosition ?: requestedPagePosition?.first ?: currentPagePosition).coerceIn(
                0,
                (items.size - 1).coerceAtLeast(0),
            )
        val item = items.getOrNull(current)
        if (
            item is ReaderUiItem.Transition &&
                item.transition is ChapterTransition.Next &&
                item.transition.to != null
        ) {
            targetPagePosition = null
            triggerLoadChapter(item.transition.to.chapter, navTarget = ChapterNavTarget.Start)
            return
        }
        if (current > 0) {
            hasMoved = true
            val target = current - 1
            targetPagePosition = target
            currentPagePosition = target
            requestedPagePosition = target to config.usePageTransitions
        } else if (item !is ReaderUiItem.Transition) {
            targetPagePosition = null
            activity.viewModel.navigateAdjacentChapter(forward = true)
        }
    }
}

/** Implementation of a vertical (top to bottom) PagerViewer. */
@Deprecated("Use ComposePagerViewer with ReaderViewModel and ReaderNavCommand instead")
class VerticalPagerViewer(activity: ReaderActivity) : PagerViewer(activity)
