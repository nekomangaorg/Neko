package eu.kanade.tachiyomi.ui.reader.model

/** Defines the target landing page when navigating to a chapter. */
sealed interface ChapterNavTarget {
    /** Navigating forward (e.g. Next chapter button, next chapter transition) -> Page 0. */
    data object Start : ChapterNavTarget

    /** Navigating backward (e.g. Prev chapter button, prev chapter transition) -> Last page. */
    data object End : ChapterNavTarget

    /**
     * Resuming a chapter (e.g. Table of Contents / chapter selector) -> saved progress or start.
     */
    data object Resume : ChapterNavTarget

    /** Resolves the target page index for the chapter given its total pages and read progress. */
    fun resolveRequestedPage(
        pageCount: Int,
        isRead: Boolean,
        lastPageRead: Int,
        pagesLeft: Int = 0,
    ): Int {
        val lastIndex = (pageCount - 1).coerceAtLeast(0)
        return when (this) {
            End -> lastIndex
            Start -> 0
            Resume -> {
                if (!isRead) {
                    lastPageRead.coerceIn(0, lastIndex)
                } else {
                    0
                }
            }
        }
    }
}
