package eu.kanade.tachiyomi.ui.reader.model

/** Represents the current lifecycle and execution state of chapter transitions in the reader. */
sealed interface ReaderChapterTransitionState {
    /** Viewer is idle and ready to process navigation. */
    data object Idle : ReaderChapterTransitionState

    /** Currently resolving and switching to a target chapter. */
    data class Loading(
        val targetChapterId: Long?,
        val navTarget: ChapterNavTarget,
    ) : ReaderChapterTransitionState

    /** Target chapter successfully loaded; waiting for initial page composition/settling. */
    data class Settling(
        val targetChapterId: Long?,
        val targetPage: Int,
    ) : ReaderChapterTransitionState

    /** Chapter transition failed (e.g. Network error, missing pages). */
    data class Error(
        val targetChapterId: Long?,
        val throwable: Throwable,
    ) : ReaderChapterTransitionState
}
