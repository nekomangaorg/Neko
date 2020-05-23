package eu.kanade.tachiyomi.ui.base

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView

class MaxHeightScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    NestedScrollView(context, attrs) {
    var maxHeight = -1

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightS = if (maxHeight > 0) {
            MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        } else {
            heightMeasureSpec
        }
        super.onMeasure(widthMeasureSpec, heightS)
    }
}
