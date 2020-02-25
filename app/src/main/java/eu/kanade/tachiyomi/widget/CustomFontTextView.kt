package eu.kanade.tachiyomi.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextView
import eu.kanade.tachiyomi.R
import java.util.HashMap
import timber.log.Timber

class CustomFontTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    TextView(context, attrs) {

    companion object {
        const val PATHWAY_GOTHIC = 0

        // Map where typefaces are cached
        private val typefaces = HashMap<Int, Typeface>(1)
    }

    init {
        if (attrs != null) {
            val values = context.obtainStyledAttributes(attrs, R.styleable.CustomFontTextView)

            val typeface = values.getInt(R.styleable.CustomFontTextView_typeface, 0)

            setTypeface(typefaces.getOrPut(typeface) {
                Typeface.createFromAsset(
                    context.assets, when (typeface) {
                        PATHWAY_GOTHIC -> "fonts/PathwayGothicOne-Regular.ttf"
                        else -> {
                            Timber.e("Font not found $typeface")
                            throw IllegalArgumentException("Font not found $typeface")
                        }
                    }
                )
            })

            values.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        // Draw two times for a more visible shadow around the text
        super.onDraw(canvas)
        super.onDraw(canvas)
    }
}
