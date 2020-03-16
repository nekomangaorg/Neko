package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import kotlin.math.max

class AutofitRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        androidx.recyclerview.widget.RecyclerView(context, attrs) {

    val manager = GridLayoutManager(context, 1)

    var columnWidth = -1
        set(value) {
            field = value
            if (value > 0 && measuredWidth > 0) {
                val count = max(1, measuredWidth / value)
                spanCount = count
                // manager.spanCount = count
            }
        }

    var spanCount = 0
        set(value) {
            field = value
            if (value > 0) {
                manager.spanCount = value
            }
        }

    val itemWidth: Int
        get() {
            return if (spanCount == 0) measuredWidth / getTempSpan()
            else measuredWidth / manager.spanCount
        }

    init {
        if (attrs != null) {
            val attrsArray = intArrayOf(android.R.attr.columnWidth)
            val array = context.obtainStyledAttributes(attrs, attrsArray)
            columnWidth = array.getDimensionPixelSize(0, -1)
            array.recycle()
        }

        layoutManager = manager
    }

    private fun getTempSpan(): Int {
        if (spanCount == 0 && columnWidth > 0) {
            return max(1, measuredWidth / columnWidth)
        }
        return 2
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        if (spanCount == 0 && columnWidth > 0) {
            val count = max(1, measuredWidth / columnWidth)
            spanCount = count
        }
    }
}
