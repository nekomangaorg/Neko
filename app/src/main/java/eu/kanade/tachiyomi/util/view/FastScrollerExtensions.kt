package eu.kanade.tachiyomi.util.view

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ViewPropertyAnimator
import androidx.core.content.ContextCompat
import com.reddit.indicatorfastscroll.FastScrollerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.dpToPxEnd
import eu.kanade.tachiyomi.util.system.getResourceColor

fun FastScrollerView.setBackground(hasBackground: Boolean) {
    background = if (hasBackground) ContextCompat.getDrawable(
        context, R.drawable.fast_scroll_background
    ) else null
    textColor = ColorStateList.valueOf(
        if (hasBackground) Color.WHITE
        else context.getResourceColor(android.R.attr.textColorPrimary)
    )
    iconColor = textColor
}

fun FastScrollerView.setStartTranslationX(shouldHide: Boolean) {
    translationX = if (shouldHide) 25f.dpToPxEnd else 0f
}

fun FastScrollerView.hide(duration: Long = 1000): ViewPropertyAnimator {
    val scrollAnim = animate().setStartDelay(duration).setDuration(250)
            .translationX(25f.dpToPxEnd)
    scrollAnim.start()
    return scrollAnim
}

fun FastScrollerView.show(animate: Boolean = true) {
    if (animate) animate().setStartDelay(0).setDuration(100).translationX(0f).start()
    else translationX = 0f
}
