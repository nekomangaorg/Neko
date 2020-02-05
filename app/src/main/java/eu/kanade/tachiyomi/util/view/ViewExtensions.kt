@file:Suppress("NOTHING_TO_INLINE")

package eu.kanade.tachiyomi.util.view

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.TextView
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.actions.hasActionButton
import com.amulyakhare.textdrawable.TextDrawable
import eu.kanade.tachiyomi.R
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.google.android.material.snackbar.Snackbar
import eu.kanade.tachiyomi.util.system.getResourceColor
import kotlin.math.min

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
fun View.snack(message: String, length: Int = Snackbar.LENGTH_SHORT, f: (Snackbar.() ->
Unit)? = null): Snackbar {
    val snack = Snackbar.make(this, message, length)
    val textView: TextView = snack.view.findViewById(com.google.android.material.R.id.snackbar_text)
    textView.setTextColor(context.getResourceColor(R.attr.snackbar_text))
   /* when {
        Build.VERSION.SDK_INT >= 23 ->  {
            val leftM = if (this is CoordinatorLayout) 0 else rootWindowInsets.systemWindowInsetLeft
            val rightM = if (this is CoordinatorLayout) 0
            else rootWindowInsets.systemWindowInsetRight
                snack.config(context, rootWindowInsets
                .systemWindowInsetBottom, rightM, leftM)
        }
        else -> snack.config(context)
    }*/
    snack.config(context)
    if (f != null) {
        snack.f()
    }
   // if (Build.VERSION.SDK_INT < 23) {
        val view = if (this !is CoordinatorLayout) this else snack.view
        view.doOnApplyWindowInsets { _, insets, _ ->
            snack.view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = 12 + insets.systemWindowInsetBottom
            }
        }
    /*}
    else {
        snack.view.doOnApplyWindowInsets { _,_,_ -> }
    }*/
    snack.show()
    return snack
}

fun View.snack(resource: Int, length: Int = Snackbar.LENGTH_SHORT, f: (Snackbar.() ->
Unit)? = null): Snackbar {
    return snack(context.getString(resource), length, f)
}

fun Snackbar.config(context: Context, bottomMargin: Int = 0, rightMargin: Int = 0, leftMargin:
Int = 0) {
    val params = this.view.layoutParams as ViewGroup.MarginLayoutParams
    params.setMargins(12 + leftMargin, 12, 12 + rightMargin, 12 + bottomMargin)
    this.view.layoutParams = params
    this.view.background = context.getDrawable(R.drawable.bg_snackbar)

    ViewCompat.setElevation(this.view, 6f)
}

fun Snackbar.getText(): CharSequence {
    val textView: TextView = view.findViewById(com.google.android.material.R.id.snackbar_text)
    return textView.text
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
    val size = min(this.width, this.height)
    return TextDrawable.builder()
            .beginConfig()
            .width(size)
            .height(size)
            .textColor(Color.WHITE)
            .useFont(Typeface.DEFAULT)
            .endConfig()
            .buildRound(text, if (random) ColorGenerator.MATERIAL.randomColor else ColorGenerator.MATERIAL.getColor(text))
}

inline val View.marginTop: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0

inline val View.marginBottom: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0

inline val View.marginRight: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.rightMargin ?: 0

inline val View.marginLeft: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin ?: 0

object RecyclerWindowInsetsListener : View.OnApplyWindowInsetsListener {
    override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
        v.setPadding(0,0,0,insets.systemWindowInsetBottom)
        //v.updatePaddingRelative(bottom = v.paddingBottom + insets.systemWindowInsetBottom)
        return insets
    }
}

fun View.doOnApplyWindowInsets(f: (View, WindowInsets, ViewPaddingState) -> Unit) {
    // Create a snapshot of the view's padding state
    val paddingState = createStateForView(this)
    setOnApplyWindowInsetsListener { v, insets ->
        f(v, insets, paddingState)
        insets
    }
    requestApplyInsetsWhenAttached()
}

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        requestApplyInsets()
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

inline fun <reified T : ViewGroup.LayoutParams> View.updateLayoutParams(block: T.() -> Unit) {
    val params = layoutParams as T
    block(params)
    layoutParams = params
}

inline fun View.updatePadding(
    @Px left: Int = paddingLeft,
    @Px top: Int = paddingTop,
    @Px right: Int = paddingRight,
    @Px bottom: Int = paddingBottom
) {
    setPadding(left, top, right, bottom)
}

private fun createStateForView(view: View) = ViewPaddingState(view.paddingLeft,
    view.paddingTop, view.paddingRight, view.paddingBottom, view.paddingStart, view.paddingEnd)

data class ViewPaddingState(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val start: Int,
    val end: Int
)

@RequiresApi(17)
inline fun View.updatePaddingRelative(
    @Px start: Int = paddingStart,
    @Px top: Int = paddingTop,
    @Px end: Int = paddingEnd,
    @Px bottom: Int = paddingBottom
) {
    setPaddingRelative(start, top, end, bottom)
}