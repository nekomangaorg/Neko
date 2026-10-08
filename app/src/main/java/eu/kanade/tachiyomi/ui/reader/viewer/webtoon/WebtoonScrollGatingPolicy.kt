package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

/**
 * Policy determining whether a page selection event across chapters should be dispatched. Strictly
 * gates adjacent chapter transitions on active user scrolling to prevent passive preload prepend
 * race conditions (Issue #3379).
 */
object WebtoonScrollGatingPolicy {

    fun shouldDispatchPageSelection(
        activeChapterId: Long?,
        candidateChapterId: Long?,
        isScrollInProgress: Boolean,
        isProgrammaticScroll: Boolean = false,
        isBackwardTransition: Boolean = true,
    ): Boolean {
        val isAdjacentChapter = activeChapterId != null && candidateChapterId != activeChapterId
        if (!isAdjacentChapter) return true
        if (isProgrammaticScroll) return true
        if (!isBackwardTransition) return true
        return isScrollInProgress
    }
}
