package eu.kanade.tachiyomi.ui.library

import android.os.Build
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.databinding.LibraryCategoryHeaderItemBinding
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign

class LibraryHeaderGestureDetector(
    private val header: LibraryHeaderHolder?,
    private val binding: LibraryCategoryHeaderItemBinding?,
) : GestureDetector.SimpleOnGestureListener() {

    var vibrated = false
    override fun onDown(e: MotionEvent?): Boolean {
        vibrated = false
        return super.onDown(e)
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float,
    ): Boolean {
        if (binding == null || header == null) return false
        val distance = ((e1?.rawX ?: 0f) - (e2?.rawX ?: 0f))
        val poa = 0.75f
        binding.categoryHeaderLayout.translationX = abs(distance).pow(poa) * -sign(distance)
        binding.rearView.isVisible = distance != 0f
        listOf(header.progressDrawableStart, header.progressDrawableEnd).forEach {
            val x = abs(binding.categoryHeaderLayout.translationX)
            val value = x / 40f.dpToPx
            it.setStartEndTrim(0f, min(MAX_PROGRESS_ANGLE, value))
            it.progressRotation = min(value, 1f)
            it.arrowScale = min(1f, value)
        }
        if (!header.locked && abs(distance) > 50) {
            header.itemView.parent?.requestDisallowInterceptTouchEvent(true)
            header.locked = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            if (!vibrated && abs(binding.categoryHeaderLayout.translationX) >= 40f.dpToPx) {
                header.itemView.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
                vibrated = true
            } else if (vibrated && abs(binding.categoryHeaderLayout.translationX) < 40f.dpToPx) {
                header.itemView.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
                vibrated = false
            }
        }
        return super.onScroll(e1, e2, distanceX, distanceY)
    }

    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ): Boolean {
        var result = false
        val diffY = e2.y - e1.y
        val diffX = e2.x - e1.x
        val mainLayout = binding?.categoryHeaderLayout ?: return false
        val animator = binding.categoryHeaderLayout.animate().setDuration(200L)
        header?.itemView?.parent?.requestDisallowInterceptTouchEvent(false)
        animator.translationX(0f)
        animator.withEndAction {
            mainLayout.translationX = 0f
            binding.rearView.isVisible = false
        }
        val x = abs(binding.categoryHeaderLayout.translationX)
        if (abs(diffX) >= abs(diffY) && sign(velocityX) == sign(diffX) &&
            ((x > 30f.dpToPx && abs(velocityX) > 100) || abs(velocityX) > 5000)
        ) {
            header?.addCategoryToUpdate()
            result = true
        }
        val wasLocked = header?.locked == true
        header?.locked = false
        animator.start()
        return result || wasLocked
    }

    private companion object {
        const val MAX_PROGRESS_ANGLE = .8f
    }
}
