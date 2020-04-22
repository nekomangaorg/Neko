@file:Suppress("NOTHING_TO_INLINE")

package eu.kanade.tachiyomi.util.view

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.graphics.Typeface
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Px
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.ColorUtils
import androidx.core.math.MathUtils.clamp
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.ThemeUtil
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import kotlinx.android.synthetic.main.main_activity.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.abs
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
fun View.snack(
    message: String,
    length: Int = Snackbar.LENGTH_SHORT,
    f: (Snackbar.() -> Unit)? = null
): Snackbar {
    val snack = Snackbar.make(this, message, length)
    if (f != null) {
        snack.f()
    }
    val theme = Injekt.get<PreferencesHelper>().theme()
    val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    if (ThemeUtil.isAMOLEDTheme(theme) && currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
        val textView: TextView =
            snack.view.findViewById(com.google.android.material.R.id.snackbar_text)
        val button: Button? =
            snack.view.findViewById(com.google.android.material.R.id.snackbar_action)
        textView.setTextColor(Color.WHITE)
        button?.setTextColor(Color.WHITE)
        snack.view.backgroundTintList = ColorStateList.valueOf(Color.DKGRAY)
    }
    snack.show()
    return snack
}

fun View.snack(
    resource: Int,
    length: Int = Snackbar.LENGTH_SHORT,
    f: (Snackbar.() -> Unit)? = null
): Snackbar {
    return snack(context.getString(resource), length, f)
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

inline fun View.visibleIf(show: Boolean) {
    visibility = if (show) View.VISIBLE else View.GONE
}

inline fun View.visInvisIf(show: Boolean) {
    visibility = if (show) View.VISIBLE else View.INVISIBLE
}

/**
 * Returns a TextDrawable determined by input
 *
 * @param text text of [TextDrawable]
 * @param random random color
 */
fun ImageView.roundTextIcon(text: String) {
    val size = min(this.width, this.height)
    val letter = text.take(1).toUpperCase()
    setImageDrawable(
        TextDrawable.builder().beginConfig().width(size).height(size).textColor(Color.WHITE)
            .useFont(Typeface.DEFAULT).endConfig().buildRound(
                letter, ColorGenerator.MATERIAL.getColor(letter)
            )
    )
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
        v.updatePaddingRelative(bottom = insets.systemWindowInsetBottom)
        // v.updatePaddingRelative(bottom = v.paddingBottom + insets.systemWindowInsetBottom)
        return insets
    }
}

object ControllerViewWindowInsetsListener : View.OnApplyWindowInsetsListener {
    override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
        v.updateLayoutParams<FrameLayout.LayoutParams> {
            val attrsArray = intArrayOf(android.R.attr.actionBarSize)
            val array = v.context.obtainStyledAttributes(attrsArray)
            topMargin = insets.systemWindowInsetTop + array.getDimensionPixelSize(0, 0)
            array.recycle()
        }
        return insets
    }
}

object HeightTopWindowInsetsListener : View.OnApplyWindowInsetsListener {
    override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
        val topInset = insets.systemWindowInsetTop
        v.setPadding(0, topInset, 0, 0)
        if (v.layoutParams.height != topInset) {
            v.layoutParams.height = topInset
            v.requestLayout()
        }
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

fun View.applyWindowInsetsForController() {
    setOnApplyWindowInsetsListener(ControllerViewWindowInsetsListener)
    requestApplyInsetsWhenAttached()
}

fun View.checkHeightThen(f: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (height > 0) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                f()
            }
        }
    })
}

fun View.applyWindowInsetsForRootController(bottomNav: View) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (bottomNav.height > 0) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                setOnApplyWindowInsetsListener { view, insets ->
                    view.updateLayoutParams<FrameLayout.LayoutParams> {
                        val attrsArray = intArrayOf(android.R.attr.actionBarSize)
                        val array = view.context.obtainStyledAttributes(attrsArray)
                        // topMargin = insets.systemWindowInsetTop + array
                        // .getDimensionPixelSize(0, 0)
                        bottomMargin = bottomNav.height
                        array.recycle()
                    }
                    insets
                }
                requestApplyInsetsWhenAttached()
            }
        }
    })
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

private fun createStateForView(view: View) = ViewPaddingState(
    view.paddingLeft,
    view.paddingTop,
    view.paddingRight,
    view.paddingBottom,
    view.paddingStart,
    view.paddingEnd
)

data class ViewPaddingState(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val start: Int,
    val end: Int
)

fun Controller.setOnQueryTextChangeListener(
    searchView: SearchView,
    onlyOnSubmit: Boolean = false,
    f: (text: String?) -> Boolean
) {
    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextChange(newText: String?): Boolean {
            if (!onlyOnSubmit && router.backstack.lastOrNull()
                    ?.controller() == this@setOnQueryTextChangeListener
            ) {
                return f(newText)
            }
            return false
        }

        override fun onQueryTextSubmit(query: String?): Boolean {
            if (router.backstack.lastOrNull()?.controller() == this@setOnQueryTextChangeListener) {
                val imm =
                    activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        ?: return f(query)
                imm.hideSoftInputFromWindow(searchView.windowToken, 0)
                return f(query)
            }
            return true
        }
    })
}

fun Controller.scrollViewWith(
    recycler: RecyclerView,
    padBottom: Boolean = false,
    customPadding: Boolean = false,
    skipFirstSnap: Boolean = false,
    swipeRefreshLayout: SwipeRefreshLayout? = null,
    afterInsets: ((WindowInsets) -> Unit)? = null
) {
    var statusBarHeight = -1
    activity?.appbar?.y = 0f
    val attrsArray = intArrayOf(android.R.attr.actionBarSize)
    val array = recycler.context.obtainStyledAttributes(attrsArray)
    var appBarHeight = if (activity!!.toolbar.height > 0) activity!!.toolbar.height
        else array.getDimensionPixelSize(0, 0)
    array.recycle()
    swipeRefreshLayout?.setDistanceToTriggerSync(150.dpToPx)
    activity!!.toolbar.post {
        if (activity!!.toolbar.height > 0) {
            appBarHeight = activity!!.toolbar.height
            recycler.requestApplyInsets()
        }
    }
    recycler.doOnApplyWindowInsets { view, insets, _ ->
        val headerHeight = insets.systemWindowInsetTop + appBarHeight
        if (!customPadding) view.updatePaddingRelative(
            top = headerHeight,
            bottom = if (padBottom) insets.systemWindowInsetBottom else view.paddingBottom
        )
        swipeRefreshLayout?.setProgressViewOffset(
            true, headerHeight + (-60).dpToPx, headerHeight + 10.dpToPx
        )
        statusBarHeight = insets.systemWindowInsetTop
        afterInsets?.invoke(insets)
    }
    var elevationAnim: ValueAnimator? = null
    var elevate = false
    val elevateFunc: (Boolean) -> Unit = { el ->
        elevate = el
        elevationAnim?.cancel()
        elevationAnim = ValueAnimator.ofFloat(
            activity!!.appbar.elevation, if (el) 15f else 0f
        )
        elevationAnim?.addUpdateListener { valueAnimator ->
            activity!!.appbar.elevation = valueAnimator.animatedValue as Float
        }
        elevationAnim?.start()
    }
    addLifecycleListener(object : Controller.LifecycleListener() {
        override fun onChangeStart(
            controller: Controller,
            changeHandler: ControllerChangeHandler,
            changeType: ControllerChangeType
        ) {
            super.onChangeStart(controller, changeHandler, changeType)
            if (changeType.isEnter)
                elevateFunc(elevate)
        }
    })
    elevateFunc(recycler.canScrollVertically(-1))
    recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (router?.backstack?.lastOrNull()
                    ?.controller() == this@scrollViewWith && statusBarHeight > -1 && activity != null && activity!!.appbar.height > 0
            ) {
                if (!recycler.canScrollVertically(-1)) {
                    val shortAnimationDuration = resources?.getInteger(
                        android.R.integer.config_shortAnimTime
                    ) ?: 0
                    activity!!.appbar.animate().y(0f).setDuration(shortAnimationDuration.toLong())
                        .start()
                    if (elevate) elevateFunc(false)
                } else {
                    activity!!.appbar.y -= dy
                    activity!!.appbar.y = clamp(
                        activity!!.appbar.y, -activity!!.appbar.height.toFloat(), 0f
                    )
                    if ((activity!!.appbar.y <= -activity!!.appbar.height.toFloat() ||
                            dy == 0 && activity!!.appbar.y == 0f) && !elevate)
                        elevateFunc(true)
                }
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                if (router?.backstack?.lastOrNull()
                        ?.controller() == this@scrollViewWith && statusBarHeight > -1 && activity != null && activity!!.appbar.height > 0
                ) {
                    val halfWay = abs((-activity!!.appbar.height.toFloat()) / 2)
                    val shortAnimationDuration = resources?.getInteger(
                        android.R.integer.config_shortAnimTime
                    ) ?: 0
                    val closerToTop = abs(activity!!.appbar.y) - halfWay > 0
                    val atTop = (!customPadding &&
                        (recycler.layoutManager as LinearLayoutManager)
                            .findFirstVisibleItemPosition() < 2 && !skipFirstSnap) ||
                        !recycler.canScrollVertically(-1)
                    activity!!.appbar.animate().y(
                            if (closerToTop && !atTop) (-activity!!.appbar.height.toFloat())
                            else 0f
                        ).setDuration(shortAnimationDuration.toLong()).start()
                    if (recycler.canScrollVertically(-1) && !elevate)
                        elevateFunc(true)
                    else if (!recycler.canScrollVertically(-1) && elevate)
                        elevateFunc(false)
                }
            }
        }
    })
}

inline fun View.updatePaddingRelative(
    @Px start: Int = paddingStart,
    @Px top: Int = paddingTop,
    @Px end: Int = paddingEnd,
    @Px bottom: Int = paddingBottom
) {
    setPaddingRelative(start, top, end, bottom)
}

fun BottomSheetDialog.setEdgeToEdge(
    activity: Activity,
    contentView: View,
    setTopMargin: Int = -1
) {
    window?.setBackgroundDrawable(null)
    val currentNightMode =
        activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    window?.navigationBarColor = activity.window.navigationBarColor
    val isLight = (activity.window?.decorView?.systemUiVisibility ?: 0) and View
        .SYSTEM_UI_FLAG_LIGHT_STATUS_BAR == View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isLight)
        window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    window?.findViewById<View>(com.google.android.material.R.id.container)?.fitsSystemWindows =
        false
    contentView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN //
    // or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

    if (activity.window.decorView.rootWindowInsets.systemWindowInsetLeft +
        activity.window.decorView.rootWindowInsets.systemWindowInsetRight == 0
    )
        contentView.systemUiVisibility = contentView.systemUiVisibility
            .or(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    if (setTopMargin > 0) (contentView.parent as View).updateLayoutParams<ViewGroup.MarginLayoutParams> {
        height =
            activity.window.decorView.height - activity.window.decorView.rootWindowInsets.systemWindowInsetTop - setTopMargin
    }
    else if (setTopMargin == 0) contentView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        topMargin = activity.window.decorView.rootWindowInsets.systemWindowInsetTop
    }
    contentView.requestLayout()
}

fun setBottomEdge(view: View, activity: Activity) {
    val marginB = view.marginBottom
    view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        bottomMargin = marginB + activity.window.decorView.rootWindowInsets.systemWindowInsetBottom
    }
}

fun SwipeRefreshLayout.setStyle() {
    setColorSchemeColors(context.getResourceColor(R.attr.actionBarTintColor))
    setProgressBackgroundColorSchemeColor(context.getResourceColor(R.attr.colorPrimaryVariant))
}

fun MaterialButton.resetStrokeColor() {
    strokeColor = ColorStateList.valueOf(
        ColorUtils.setAlphaComponent(
            context.getResourceColor(
                R.attr.colorOnSurface
            ), 31
        )
    )
}
