package eu.kanade.tachiyomi.ui.base

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView

class MaxHeightScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    NestedScrollView(context, attrs) {
    var maxHeight = -1

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightS = if (maxHeight > 0) {
            MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        } else {
            heightMeasureSpec
        }
        if (maxHeight < height + (rootWindowInsets?.systemWindowInsetBottom ?: 0)) {
            heightS = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        }
        super.onMeasure(widthMeasureSpec, heightS)
    }
}
