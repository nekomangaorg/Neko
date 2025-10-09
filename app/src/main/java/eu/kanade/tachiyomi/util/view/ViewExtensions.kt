@file:Suppress("NOTHING_TO_INLINE")

package eu.kanade.tachiyomi.util.view

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import androidx.annotation.IdRes
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.forEach
import androidx.core.view.updatePaddingRelative
import com.google.android.material.navigation.NavigationBarItemView
import com.google.android.material.navigation.NavigationBarMenuView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.snackbar.Snackbar
import eu.kanade.tachiyomi.util.lang.tintText
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.widget.cascadeMenuStyler
import me.saket.cascade.CascadePopupMenu
import org.nekomanga.R

/**
 * Shows a snackbar in this view.
 *
 * @param message the message to show.
 * @param length the duration of the snack.
 * @param f a function to execute in the snack, allowing for example to define a custom action.
 */
fun View.snack(
    message: CharSequence,
    length: Int = Snackbar.LENGTH_SHORT,
    f: (Snackbar.() -> Unit)? = null,
): Snackbar {
    val snack = Snackbar.make(this, message, length)
    if (f != null) {
        snack.f()
    }
    snack.show()
    return snack
}

object RecyclerWindowInsetsListener : View.OnApplyWindowInsetsListener {
    override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
        v.updatePaddingRelative(
            bottom = WindowInsetsCompat.toWindowInsetsCompat(insets).getInsets(systemBars()).bottom
        )
        return insets
    }
}

fun View.doOnApplyWindowInsetsCompat(f: (View, WindowInsetsCompat, ViewPaddingState) -> Unit) {
    // Create a snapshot of the view's padding state
    val paddingState = createStateForView(this)
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        f(v, insets, paddingState)
        insets
    }
    requestApplyInsetsWhenAttached()
}

fun View.checkHeightThen(f: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(
        object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (height > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    f()
                }
            }
        }
    )
}

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        requestApplyInsets()
    } else {
        addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    v.requestApplyInsets()
                }

                override fun onViewDetachedFromWindow(v: View) = Unit
            }
        )
    }
}

private fun createStateForView(view: View) =
    ViewPaddingState(
        view.paddingLeft,
        view.paddingTop,
        view.paddingRight,
        view.paddingBottom,
        view.paddingStart,
        view.paddingEnd,
    )

data class ViewPaddingState(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val start: Int,
    val end: Int,
)

@SuppressLint("RestrictedApi")
fun NavigationBarView.getItemView(@IdRes id: Int): NavigationBarItemView? {
    val order = (menu as MenuBuilder).findItemIndex(id)
    return (getChildAt(0) as NavigationBarMenuView).getChildAt(order) as? NavigationBarItemView
}

var View.compatToolTipText: CharSequence?
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tooltipText
        } else {
            ""
        }
    set(value) {
        ViewCompat.setTooltipText(this, value)
    }

@SuppressLint("RestrictedApi")
inline fun View.popupMenu(
    items: List<Pair<Int, Int>>,
    selectedItemId: Int? = null,
    noinline onMenuItemClick: MenuItem.() -> Unit,
): CascadePopupMenu {
    val popup =
        CascadePopupMenu(context, this, Gravity.NO_GRAVITY, styler = cascadeMenuStyler(context))
    items.forEach { (id, stringRes) -> popup.menu.add(0, id, 0, stringRes) }

    if (selectedItemId != null) {
        val blendedAccent =
            ColorUtils.blendARGB(
                context.getResourceColor(R.attr.colorSecondary),
                context.getResourceColor(R.attr.colorOnBackground),
                0.5f,
            )
        (popup.menu as? MenuBuilder)?.setOptionalIconsVisible(true)
        val emptyIcon = ContextCompat.getDrawable(context, R.drawable.ic_blank_24dp)
        popup.menu.forEach { item ->
            item.icon =
                when (item.itemId) {
                    selectedItemId ->
                        ContextCompat.getDrawable(context, R.drawable.ic_check_24dp)
                            ?.mutate()
                            ?.apply { setTint(blendedAccent) }
                    else -> emptyIcon
                }
            if (item.itemId == selectedItemId) {
                item.title = item.title?.tintText(blendedAccent)
            }
        }
    }

    popup.setOnMenuItemClickListener {
        it.onMenuItemClick()
        true
    }

    popup.show()
    return popup
}

var View.backgroundColor: Int?
    get() = (background as? ColorDrawable)?.color
    set(value) {
        if (value != null) setBackgroundColor(value) else background = null
    }

fun View?.isVisibleOnScreen(): Boolean {
    if (this == null) {
        return false
    }
    if (!this.isShown) {
        return false
    }
    val actualPosition = Rect()
    this.getGlobalVisibleRect(actualPosition)
    val screen =
        Rect(
            0,
            0,
            Resources.getSystem().displayMetrics.widthPixels,
            Resources.getSystem().displayMetrics.heightPixels,
        )
    return actualPosition.intersect(screen)
}
