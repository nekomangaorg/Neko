package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import eu.kanade.tachiyomi.ui.base.MaterialFastScroll
import eu.kanade.tachiyomi.util.system.dpToPxEnd
import eu.kanade.tachiyomi.util.system.isLTR
import kotlin.math.abs
import kotlin.math.min

class LibraryFastScroll @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialFastScroll(context, attrs) {

    private var currentX = 0f

    private val isSingleCategory: Boolean
        get() = (recyclerView.adapter as? LibraryCategoryAdapter)?.isSingleCategory ?: true

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isSingleCategory) return super.onTouchEvent(event)
        val oldFastScroll = categoryFastScroll
        currentX = event.x
        if (categoryFastScroll != oldFastScroll && canScroll) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        return super.onTouchEvent(event)
    }

    private val categoryFastScroll: Boolean
        get() = abs(currentX - handle.x) >= min(200, width / 2)

    override fun setRecyclerViewPosition(y: Float) {
        if (recyclerView != null) {
            val adapter = recyclerView.adapter as? LibraryCategoryAdapter ?: return super.setRecyclerViewPosition(y)
            if (adapter.isSingleCategory || categoryFastScroll) {
                return super.setRecyclerViewPosition(y)
            }
            var targetPos = getTargetPos(y)
            val category = when (val item = adapter.getItem(targetPos)) {
                is LibraryItem -> {
                    targetPos = adapter.currentItems.indexOf(item.header)
                    item.header.category
                }
                is LibraryHeaderItem -> item.category
                else -> return super.setRecyclerViewPosition(y)
            }
            adapter.controller?.scrollToCategory(category)
            updateBubbleText(targetPos)
        }
    }

    override fun setBubbleAndHandlePosition(y: Float) {
        super.setBubbleAndHandlePosition(y)
        if (bubbleEnabled && !isSingleCategory) {
            bubble.translationX =
                if (categoryFastScroll) {
                250f * (if (resources.isLTR) -1 else 1)
            } else {
                currentX - handle.x
            } + (-45f).dpToPxEnd(resources)
        }
    }
}
