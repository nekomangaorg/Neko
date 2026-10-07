package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Pointers held on a control inside a tap navigation area, such as the Retry button.
 * [pagerTapNavigation] and [webtoonTapNavigation] read the tap in the Initial pass, before the
 * control consumes it, so they skip a tap whose down pointer was claimed here. Compose gives every
 * new pointer a fresh id, so an old claim never matches a later tap.
 */
@Stable
class TapNavigationClaim {
    private val claimed = mutableSetOf<PointerId>()

    fun claim(id: PointerId) {
        claimed += id
    }

    fun release(id: PointerId) {
        claimed -= id
    }

    fun isClaimed(id: PointerId): Boolean = id in claimed
}

/**
 * Claims the first pointer of each gesture on this element, so tap navigation ignores its taps. The
 * claim is released in the Final pass of the up event, after the tap navigation ancestors have read
 * it in the Initial pass.
 */
fun Modifier.claimTapNavigation(claim: TapNavigationClaim?): Modifier =
    if (claim == null) {
        this
    } else {
        pointerInput(claim) {
            awaitEachGesture {
                val down =
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                claim.claim(down.id)
                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Final)
                } while (event.changes.any { it.id == down.id && it.pressed })
                claim.release(down.id)
            }
        }
    }
