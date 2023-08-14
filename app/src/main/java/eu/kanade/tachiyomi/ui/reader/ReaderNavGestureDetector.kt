package eu.kanade.tachiyomi.ui.reader

import android.view.GestureDetector
import android.view.MotionEvent
import eu.kanade.tachiyomi.util.view.collapse
import eu.kanade.tachiyomi.util.view.expand
import kotlin.math.abs

class ReaderNavGestureDetector(private val activity: ReaderActivity) : GestureDetector
.SimpleOnGestureListener() {

    var hasScrollHorizontal = false
    var lockVertical = false

    override fun onDown(e: MotionEvent): Boolean {
        lockVertical = false
        hasScrollHorizontal = false
        return false
    }

    override fun onScroll(
        e: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float,
    ): Boolean {
        val e1 = e ?: return false
        val newDistanceX = e1.rawX - e2.rawX
        val newDistanceY = e1.rawY - e2.rawY
        if ((!hasScrollHorizontal || lockVertical)) {
            hasScrollHorizontal = abs(newDistanceX) > abs(newDistanceY) && abs(newDistanceX) > 40
            if (!lockVertical) {
                lockVertical = abs(newDistanceX) < abs(newDistanceY) && abs(newDistanceY) > 150
            }
        }
        return !hasScrollHorizontal && lockVertical
    }

    override fun onFling(
        e: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ): Boolean {
        val e1 = e ?: return false
        var result = false
        val diffY = e2.y - e1.y
        val diffX = e2.x - e1.x
        val sheetBehavior = activity.binding.chaptersSheet.root.sheetBehavior
        if (!hasScrollHorizontal && abs(diffX) < abs(diffY) &&
            (abs(diffY) > SWIPE_THRESHOLD || abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) &&
            diffY <= 0
        ) {
            lockVertical = true
            sheetBehavior?.expand()
            result = true
        }

        if (!result) {
            activity.binding.chaptersSheet.root.sheetBehavior?.collapse()
        }
        return result
    }

    private companion object {
        const val SWIPE_THRESHOLD = 50
        const val SWIPE_VELOCITY_THRESHOLD = 100
    }
}
