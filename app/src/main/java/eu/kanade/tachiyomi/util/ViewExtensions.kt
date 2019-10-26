@file:Suppress("NOTHING_TO_INLINE")

package eu.kanade.tachiyomi.util

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.Typeface
import android.os.Build
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewCompat
import android.support.v4.view.WindowInsetsCompat
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.widget.TextView
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.main.doOnApplyWindowInsets
import eu.kanade.tachiyomi.ui.main.marginBottom
import eu.kanade.tachiyomi.ui.main.updateLayoutParams
import eu.kanade.tachiyomi.ui.main.updatePaddingRelative

/**
 * Returns coordinates of view.
 * Used for animation
 *
 * @return coordinates of view
 */
fun View.getCoordinates() = Point((left + right) / 2, (top + bottom) / 2)

/**
 * Shows a snackbar in this view.
 *
 * @param message the message to show.
 * @param length the duration of the snack.
 * @param f a function to execute in the snack, allowing for example to define a custom action.
 */
inline fun View.snack(message: String, length: Int = Snackbar.LENGTH_LONG, f: Snackbar.() -> Unit): Snackbar {
    val snack = Snackbar.make(this, message, length)
    val textView: TextView = snack.view.findViewById(android.support.design.R.id.snackbar_text)
    textView.setTextColor(Color.WHITE)
    when {
        Build.VERSION.SDK_INT >= 23 -> snack.config(context, rootWindowInsets.systemWindowInsetBottom)
        else -> snack.config(context)
    }
    snack.f()
    snack.show()
    return snack
}

fun Snackbar.config(context: Context, bottomMargin: Int = 0) {
    val params = this.view.layoutParams as ViewGroup.MarginLayoutParams
    params.setMargins(12, 12, 12, 12 + bottomMargin)
    this.view.layoutParams = params

    this.view.background = context.getDrawable(R.drawable.bg_snackbar)

    ViewCompat.setElevation(this.view, 6f)
}

inline fun View.visible() {
    visibility = View.VISIBLE
}

inline fun View.invisible() {
    visibility = View.INVISIBLE
}

inline fun View.gone() {
    visibility = View.GONE
}

inline fun View.visibleIf(block: () -> Boolean) {
    visibility = if (block()) View.VISIBLE else View.GONE
}

/**
 * Returns a TextDrawable determined by input
 *
 * @param text text of [TextDrawable]
 * @param random random color
 */
fun View.getRound(text: String, random : Boolean = true): TextDrawable {
    val size = Math.min(this.width, this.height)
    return TextDrawable.builder()
            .beginConfig()
            .width(size)
            .height(size)
            .textColor(Color.WHITE)
            .useFont(Typeface.DEFAULT)
            .endConfig()
            .buildRound(text, if (random) ColorGenerator.MATERIAL.randomColor else ColorGenerator.MATERIAL.getColor(text))
}
