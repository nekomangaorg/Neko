package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Last pointer that went down on a control inside a tap navigation area, such as the Retry button.
 * [pagerTapNavigation] and [webtoonTapNavigation] read the tap in the Initial pass, before the
 * control consumes it, so they skip a tap whose down pointer was claimed here. Compose gives every
 * new pointer a fresh id, so an old claim never matches a later tap.
 */
@Stable
class TapNavigationClaim {
    private var claimed: PointerId? = null

    fun claim(id: PointerId) {
        claimed = id
    }

    fun isClaimed(id: PointerId): Boolean = claimed == id
}

/** Claims every pointer that goes down on this element, so tap navigation ignores its taps. */
fun Modifier.claimTapNavigation(claim: TapNavigationClaim?): Modifier =
    if (claim == null) {
        this
    } else {
        pointerInput(claim) {
            awaitEachGesture {
                val down =
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                claim.claim(down.id)
            }
        }
    }
