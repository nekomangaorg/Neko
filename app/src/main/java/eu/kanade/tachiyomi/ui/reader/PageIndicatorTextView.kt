package eu.kanade.tachiyomi.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import androidx.appcompat.widget.AppCompatTextView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ScaleXSpan
import android.util.AttributeSet
import eu.kanade.tachiyomi.util.OutlineSpan

/**
 * Page indicator found at the bottom of the reader
 */
class PageIndicatorTextView(
        context: Context,
        attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    init {
        setTextColor(Color.rgb(235, 235, 235))
    }

    @SuppressLint("SetTextI18n")
    override fun setText(text: CharSequence?, type: BufferType?) {
        // Add spaces at the start & end of the text, otherwise the stroke is cut-off because it's
        // not taken into account when measuring the text (view's padding doesn't help).
        val currText = " $text "

        // Also add a bit of spacing between each character, as the stroke overlaps them
        val finalText = SpannableString(currText.asIterable().joinToString("\u00A0")).apply {
            // Apply text outline
            setSpan(spanOutline, 1, length-1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            for (i in 1..lastIndex step 2) {
                setSpan(ScaleXSpan(0.3f), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        super.setText(finalText, BufferType.SPANNABLE)
    }

    private companion object {
        // A span object with text outlining properties
        val spanOutline = OutlineSpan(
                strokeColor = Color.rgb(45, 45, 45),
                strokeWidth = 4f
        )
    }
}
