package eu.kanade.tachiyomi.ui.base

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.view.marginTop
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import eu.davidea.fastscroller.FastScroller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.dpToPxEnd
import eu.kanade.tachiyomi.util.system.isLTR
import kotlin.math.abs

@Suppress("LeakingThis")
open class MaterialFastScroll @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FastScroller(context, attrs) {

    var canScroll = false
    var startY = -1f
    var scrollOffset = 0
    var controller: BaseController<*>? = null
    init {
        setViewsToUse(
            R.layout.material_fastscroll,
            R.id.fast_scroller_bubble,
            R.id.fast_scroller_handle,
        )
        autoHideEnabled = true
        ignoreTouchesOutsideHandle = false
        updateScrollListener()
    }

    val isFastScrolling: Boolean
        get() = handle.isSelected

    // Overriding to force a distance moved before scrolling
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (controller?.isDragging == true ||
            recyclerView.computeVerticalScrollRange() <= recyclerView.computeVerticalScrollExtent()
        ) {
            return if (startY > -1f || controller?.isDragging == true) {
                dispatchTouchToRecycler(event)
                false
            } else {
                super.onTouchEvent(event)
            }
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (
                    if (context.resources.isLTR) {
                        event.x < handle.x - handle.paddingStart
                    } else {
                        event.x > handle.width + handle.paddingStart
                    }
                ) {
                    return false
                }
                val y = event.y
                startY = event.y
                if (canScroll) {
                    handle.isSelected = true
                    notifyScrollStateChange(true)
                    showBubble()
                    showScrollbar()
                    setBubbleAndHandlePosition(y)
                    setRecyclerViewPosition(y)
                    return true
                }
                dispatchTouchToRecycler(event)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val y = event.y
                if (!canScroll && abs(y - startY) > 15.dpToPx) {
                    canScroll = true
                    handle.isSelected = true
                    notifyScrollStateChange(true)
                    showBubble()
                    showScrollbar()
                    dispatchTouchToRecycler(event) { action = MotionEvent.ACTION_CANCEL }
                }
                if (canScroll) {
                    setBubbleAndHandlePosition(y)
                    setRecyclerViewPosition(y)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                startY = -1f
                if (!canScroll) {
                    dispatchTouchToRecycler(event)
                }
                canScroll = false
            }
        }
        return super.onTouchEvent(event)
    }

    private fun dispatchTouchToRecycler(event: MotionEvent, block: (MotionEvent.() -> Unit)? = null) {
        val ev2 = MotionEvent.obtain(event)
        ev2.offsetLocation(this.x, this.y)
        block?.invoke(ev2)
        recyclerView.dispatchTouchEvent(ev2)
        ev2.recycle()
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
                    targetPos,
                    scrollOffset,
                )
            } else {
                (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    targetPos,
                    scrollOffset,
                )
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
