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

/**
 * Attempts to pan the active zoomed page in the direction corresponding to a step navigation.
 * Returns `true` if a pan was executed, or `false` if the page boundary was reached.
 */
fun PagerPanDelegate.tryStepPan(isVertical: Boolean, isRtl: Boolean, forward: Boolean): Boolean {
    return if (isVertical) {
        if (forward) {
            if (canPanDown()) {
                panDown()
                true
            } else {
                false
            }
        } else {
            if (canPanUp()) {
                panUp()
                true
            } else {
                false
            }
        }
    } else if (isRtl) {
        if (forward) {
            if (canPanLeft()) {
                panLeft()
                true
            } else {
                false
            }
        } else {
            if (canPanRight()) {
                panRight()
                true
            } else {
                false
            }
        }
    } else {
        if (forward) {
            if (canPanRight()) {
                panRight()
                true
            } else {
                false
            }
        } else {
            if (canPanLeft()) {
                panLeft()
                true
            } else {
                false
            }
        }
    }
}
