package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import eu.davidea.fastscroller.FastScroller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.dpToPxEnd
import eu.kanade.tachiyomi.util.view.marginTop
import kotlin.math.abs

class MaterialFastScroll @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FastScroller(context, attrs) {

    var scrollOffset = 0
    init {
        setViewsToUse(
            R.layout.material_fastscroll, R.id.fast_scroller_bubble, R.id.fast_scroller_handle
        )
        autoHideEnabled = true
        ignoreTouchesOutsideHandle = true
        updateScrollListener()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isHidden) return false
        return super.onTouchEvent(event)
    }

    override fun setBubbleAndHandlePosition(y: Float) {
        super.setBubbleAndHandlePosition(y)
        if (bubbleEnabled) {
            bubble.y = handle.y - bubble.height / 2f + handle.height / 2f
            bubble.translationX = (-45f).dpToPxEnd
        }
    }

    override fun setRecyclerViewPosition(y: Float) {
        if (recyclerView != null) {
            val targetPos = getTargetPos(y)
            if (layoutManager is StaggeredGridLayoutManager) {
                (layoutManager as StaggeredGridLayoutManager).scrollToPositionWithOffset(
                    targetPos, scrollOffset
                )
            } else {
                (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(targetPos, scrollOffset)
            }
            updateBubbleText(targetPos)
        }
    }

    private fun updateScrollListener() {
        onScrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!isEnabled || bubble == null || handle.isSelected) return
                val verticalScrollOffset = recyclerView.computeVerticalScrollOffset()
                val verticalScrollRange = recyclerView.computeVerticalScrollRange() - marginTop
                val proportion =
                    verticalScrollOffset.toFloat() / (verticalScrollRange - height).toFloat()
                setBubbleAndHandlePosition(height * proportion)
                // If scroll amount is small, don't show it
                if (minimumScrollThreshold == 0 || dy == 0 || abs(dy) > minimumScrollThreshold || scrollbarAnimator.isAnimating) {
                    showScrollbar()
                    if (autoHideEnabled) hideScrollbar()
                }
            }
        }
    }
}
