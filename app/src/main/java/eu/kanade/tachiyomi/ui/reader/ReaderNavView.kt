package eu.kanade.tachiyomi.ui.reader

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout

class ReaderNavView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ConstraintLayout(context, attrs) {
    override fun canScrollVertically(direction: Int): Boolean {
        return true
    }

    override fun shouldDelayChildPressedState(): Boolean {
        return true
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun getScrollCaptureHint(): Int {
        return SCROLL_CAPTURE_HINT_EXCLUDE
    }
}
