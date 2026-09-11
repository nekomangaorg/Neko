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
}
