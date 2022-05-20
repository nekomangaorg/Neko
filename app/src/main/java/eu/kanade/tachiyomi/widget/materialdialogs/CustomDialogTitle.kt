package eu.kanade.tachiyomi.widget.materialdialogs

import android.content.Context
import android.text.Layout
import android.util.AttributeSet
import android.util.TypedValue
import com.google.android.material.textview.MaterialTextView
import eu.kanade.tachiyomi.R

class CustomDialogTitle constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialTextView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val layout: Layout? = layout
        if (layout != null) {
            val lineCount = layout.lineCount
            if (lineCount > 0) {
                val ellipsisCount = layout.getEllipsisCount(lineCount - 1)
                if (ellipsisCount > 0) {
                    isSingleLine = false
                    maxLines = 2
                    val a = context.obtainStyledAttributes(
                        null,
                        R.styleable.TextAppearance,
                        android.R.attr.textAppearanceMedium,
                        android.R.style.TextAppearance_Medium,
                    )
                    a.getDimensionPixelSize(
                        R.styleable.TextAppearance_android_textSize,
                        0,
                    ).takeIf { it != 0 }?.let { textSize ->
                        // textSize is already expressed in pixels
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
                    }
                    a.recycle()
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                }
            }
        }
    }
}
