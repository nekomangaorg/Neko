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
    return when {
        isVertical && forward -> tryPan(::canPanDown, ::panDown)
        isVertical && !forward -> tryPan(::canPanUp, ::panUp)
        isRtl && forward -> tryPan(::canPanLeft, ::panLeft)
        isRtl && !forward -> tryPan(::canPanRight, ::panRight)
        !isRtl && forward -> tryPan(::canPanRight, ::panRight)
        !isRtl && !forward -> tryPan(::canPanLeft, ::panLeft)
        else -> false
    }
}

private inline fun tryPan(canPan: () -> Boolean, pan: () -> Unit): Boolean {
    return if (canPan()) {
        pan()
        true
    } else {
        false
    }
}
