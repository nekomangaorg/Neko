package eu.kanade.tachiyomi.ui.reader.viewer.pager

/** Delegate interface for panning zoomed pages in paginated reading modes. */
interface PagerPanDelegate {
    fun canPanLeft(): Boolean = false

    fun canPanRight(): Boolean = false

    fun canPanUp(): Boolean = false

    fun canPanDown(): Boolean = false

    fun panLeft() {}

    fun panRight() {}

    fun panUp() {}

    fun panDown() {}
}
