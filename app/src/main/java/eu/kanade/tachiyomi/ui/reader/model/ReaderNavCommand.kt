package eu.kanade.tachiyomi.ui.reader.model

/** Unidirectional programmatic navigation commands dispatched to reader Compose viewers. */
sealed interface ReaderNavCommand {
    data class ScrollToPage(
        val pageIndex: Int,
        val animated: Boolean = true,
        val chapterId: Long? = null,
    ) : ReaderNavCommand

    data class SnapToPage(
        val pageIndex: Int,
        val chapterId: Long? = null,
    ) : ReaderNavCommand

    data class ScrollToItem(
        val itemIndex: Int,
        val animated: Boolean = true,
    ) : ReaderNavCommand

    data class StepPage(val forward: Boolean) : ReaderNavCommand

    data class ScrollByDelta(val delta: Float) : ReaderNavCommand
}
