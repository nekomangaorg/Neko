@file:Suppress("NOTHING_TO_INLINE")

package eu.kanade.tachiyomi.util.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.PowerManager
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.annotation.FloatRange
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.children
import androidx.core.view.descendants
import androidx.core.view.forEach
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.math.MathUtils
import com.google.android.material.navigation.NavigationBarItemView
import com.google.android.material.navigation.NavigationBarMenuView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.snackbar.Snackbar
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.lang.tintText
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.isLTR
import eu.kanade.tachiyomi.util.system.powerManager
import eu.kanade.tachiyomi.util.system.pxToDp
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import eu.kanade.tachiyomi.widget.StaggeredGridLayoutManagerAccurateOffset
import eu.kanade.tachiyomi.widget.cascadeMenuStyler
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import me.saket.cascade.CascadePopupMenu

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
    f: (Snackbar.() -> Unit)? = null,
): Snackbar {
    val snack = Snackbar.make(this, message, length)
    if (f != null) {
        snack.f()
    }
    snack.show()
    return snack
}

fun View.snack(
    resource: Int,
    length: Int = Snackbar.LENGTH_SHORT,
    f: (Snackbar.() -> Unit)? = null,
): Snackbar {
    return snack(context.getString(resource), length, f)
}

fun Snackbar.getText(): CharSequence {
    val textView: TextView = view.findViewById(com.google.android.material.R.id.snackbar_text)
    return textView.text
}

object RecyclerWindowInsetsListener : View.OnApplyWindowInsetsListener {
    override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
        v.updatePaddingRelative(
            bottom = WindowInsetsCompat.toWindowInsetsCompat(insets)
                .getInsets(systemBars()).bottom,
        )
        return insets
    }
}

fun View.applyBottomAnimatedInsets(
    bottomMargin: Int = 0,
    setPadding: Boolean = false,
    onApplyInsets: ((View, WindowInsetsCompat) -> Unit)? = null,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    val setInsets: ((WindowInsetsCompat) -> Unit) = { insets ->
        val bottom = insets.getInsets(systemBars() or ime()).bottom

        if (setPadding) {
            updatePaddingRelative(bottom = bottomMargin + bottom)
        } else {
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                this.bottomMargin = bottom + bottomMargin
            }
        }
    }
    var handleInsets = true
    doOnApplyWindowInsetsCompat { view, insets, _ ->
        onApplyInsets?.invoke(view, insets)
        if (handleInsets) {
            setInsets(insets)
        }
    }

    ViewCompat.setWindowInsetsAnimationCallback(
        this,
        object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
            override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                handleInsets = false
                super.onPrepare(animation)
            }

            override fun onStart(
                animation: WindowInsetsAnimationCompat,
                bounds: WindowInsetsAnimationCompat.BoundsCompat,
            ): WindowInsetsAnimationCompat.BoundsCompat {
                handleInsets = false
                rootWindowInsetsCompat?.let { insets -> setInsets(insets) }
                return super.onStart(animation, bounds)
            }

            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: List<WindowInsetsAnimationCompat>,
            ): WindowInsetsCompat {
                setInsets(insets)
                return insets
            }

            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                handleInsets = true
                rootWindowInsetsCompat?.let { insets -> setInsets(insets) }
            }
        },
    )
}

class ControllerViewWindowInsetsListener(private val topHeight: Int) : OnApplyWindowInsetsListener {
    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        v.updateLayoutParams<FrameLayout.LayoutParams> {
            topMargin = insets.getInsets(systemBars()).top + topHeight
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

fun View.doOnApplyWindowInsetsCompat(f: (View, WindowInsetsCompat, ViewPaddingState) -> Unit) {
    // Create a snapshot of the view's padding state
    val paddingState = createStateForView(this)
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        f(v, insets, paddingState)
        insets
    }
    requestApplyInsetsWhenAttached()
}

fun View.applyWindowInsetsForController(topHeight: Int) {
    ViewCompat.setOnApplyWindowInsetsListener(this, ControllerViewWindowInsetsListener(topHeight))
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
        },
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
            },
        )
    }
}

private fun createStateForView(view: View) = ViewPaddingState(
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

fun setBottomEdge(view: View, activity: Activity) {
    val marginB = view.marginBottom
    view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        bottomMargin = marginB +
            (
                activity.window.decorView.rootWindowInsetsCompat?.getInsets(systemBars())
                    ?.bottom ?: 0
                )
    }
}

fun SwipeRefreshLayout.setStyle() {
    setColorSchemeColors(context.getResourceColor(R.attr.actionBarTintColor))
    setProgressBackgroundColorSchemeColor(context.getResourceColor(R.attr.colorPrimaryVariant))
}

fun MaterialButton.resetStrokeColor() {
    strokeColor = ColorStateList.valueOf(
        ColorUtils.setAlphaComponent(context.getResourceColor(R.attr.colorOnSurface), 31),
    )
}

@SuppressLint("RestrictedApi")
fun NavigationBarView.getItemView(@IdRes id: Int): NavigationBarItemView? {
    val order = (menu as MenuBuilder).findItemIndex(id)
    return (getChildAt(0) as NavigationBarMenuView).getChildAt(order) as? NavigationBarItemView
}

fun RecyclerView.smoothScrollToTop() {
    val linearLayoutManager = layoutManager as? LinearLayoutManager
    val staggeredLayoutManager = layoutManager as? StaggeredGridLayoutManagerAccurateOffset
    if (linearLayoutManager != null || staggeredLayoutManager != null) {
        val smoothScroller: SmoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }
        }
        smoothScroller.targetPosition = 0
        val firstItemPos = linearLayoutManager?.findFirstVisibleItemPosition()
            ?: staggeredLayoutManager?.findFirstVisibleItemPosition() ?: 0
        if (firstItemPos > 15) {
            scrollToPosition(15)
            post {
                linearLayoutManager?.startSmoothScroll(smoothScroller)
                staggeredLayoutManager?.startSmoothScroll(smoothScroller)
            }
        } else {
            linearLayoutManager?.startSmoothScroll(smoothScroller)
            staggeredLayoutManager?.startSmoothScroll(smoothScroller)
        }
    } else {
        scrollToPosition(0)
    }
}

fun View.rowsForValue(value: Float) = measuredWidth.numberOfRowsForValue(value)

fun Int.numberOfRowsForValue(rawValue: Float): Int {
    val value = (rawValue / 2f) - .5f
    val size = 1.5f.pow(value)
    val trueSize =
        AutofitRecyclerView.MULTIPLE * ((size * 100 / AutofitRecyclerView.MULTIPLE).roundToInt()) / 100f
    val dpWidth = (this.pxToDp / 100f).roundToInt()
    return max(1, (dpWidth / trueSize).roundToInt())
}

fun Int.numberOfColumnsForCompose(rawValue: Float): Int {
    val size = 1.5f.pow(rawValue)
    val trueSize =
        AutofitRecyclerView.MULTIPLE * ((size * 100 / AutofitRecyclerView.MULTIPLE).roundToInt()) / 100f
    val dpWidth = (this.pxToDp / 100f).roundToInt()
    return max(1, (dpWidth / trueSize).roundToInt())
}

var View.compatToolTipText: CharSequence?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    items.forEach { (id, stringRes) ->
        popup.menu.add(0, id, 0, stringRes)
    }

    if (selectedItemId != null) {
        val blendedAccent = ColorUtils.blendARGB(
            context.getResourceColor(R.attr.colorSecondary),
            context.getResourceColor(R.attr.colorOnBackground),
            0.5f,
        )
        (popup.menu as? MenuBuilder)?.setOptionalIconsVisible(true)
        val emptyIcon = ContextCompat.getDrawable(context, R.drawable.ic_blank_24dp)
        popup.menu.forEach { item ->
            item.icon = when (item.itemId) {
                selectedItemId -> ContextCompat.getDrawable(context, R.drawable.ic_check_24dp)
                    ?.mutate()?.apply {
                        setTint(blendedAccent)
                    }
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

fun MaterialCardView.makeShapeCorners(
    @Dimension topStart: Float = 0f,
    @Dimension bottomEnd: Float = 0f,
): ShapeAppearanceModel {
    return shapeAppearanceModel.toBuilder()
        .apply {
            if (context.resources.isLTR) {
                setTopLeftCorner(CornerFamily.ROUNDED, topStart)
                setBottomLeftCorner(CornerFamily.ROUNDED, if (topStart > 0) 4f.dpToPx else 0f)
                setBottomRightCorner(CornerFamily.ROUNDED, bottomEnd)
                setTopRightCorner(CornerFamily.ROUNDED, if (bottomEnd > 0) 4f.dpToPx else 0f)
            } else {
                setTopLeftCorner(CornerFamily.ROUNDED, if (topStart > 0) 4f.dpToPx else 0f)
                setBottomLeftCorner(CornerFamily.ROUNDED, topStart)
                setBottomRightCorner(CornerFamily.ROUNDED, if (bottomEnd > 0) 4f.dpToPx else 0f)
                setTopRightCorner(CornerFamily.ROUNDED, bottomEnd)
            }
        }
        .build()
}

fun setCards(
    showOutline: Boolean,
    mainCard: MaterialCardView,
    badgeView: MaterialCardView?,
) {
    badgeView?.strokeWidth = if (showOutline) 0.75f.dpToPx.toInt() else 0
    badgeView?.cardElevation = if (showOutline) 0f else 3f.dpToPx
    mainCard.strokeWidth = if (showOutline) 1.dpToPx else 0
}

var View.backgroundColor: Int?
    get() = (background as? ColorDrawable)?.color
    set(value) {
        if (value != null) setBackgroundColor(value) else background = null
    }

/**
 * Returns this ViewGroup's first descendant of specified class
 */
inline fun <reified T> ViewGroup.findChild(): T? {
    return children.find { it is T } as? T
}

/**
 * Returns this ViewGroup's first descendant of specified class
 */
inline fun <reified T> ViewGroup.findDescendant(): T? {
    return descendants.find { it is T } as? T
}

fun Dialog.blurBehindWindow(
    window: Window?,
    blurAmount: Float = 20f,
    onShow: DialogInterface.OnShowListener? = null,
    onDismiss: DialogInterface.OnDismissListener? = null,
    onCancel: DialogInterface.OnCancelListener? = null,
) {
    var supportsBlur = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window?.windowManager?.isCrossWindowBlurEnabled == true) {
        supportsBlur = true
    }
    var registered = true
    val powerSaverChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window?.windowManager?.isCrossWindowBlurEnabled == true) {
                return
            }
            val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !this@blurBehindWindow.context.powerManager.isPowerSaveMode
            window?.setDimAmount(if (canBlur) 0.45f else 0.77f)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
            if (canBlur) {
                window?.decorView?.setRenderEffect(
                    RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP),
                )
            } else {
                window?.decorView?.setRenderEffect(null)
            }
        }
    }
    val filter = IntentFilter()
    filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
    context.registerReceiver(powerSaverChangeReceiver, filter)
    val unregister: () -> Unit = {
        if (registered) {
            context.unregisterReceiver(powerSaverChangeReceiver)
            registered = false
        }
    }
    setOnShowListener {
        onShow?.onShow(it)
        if (!supportsBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window?.decorView?.animateBlur(1f, blurAmount, 50)?.start()
        }
    }
    setOnDismissListener {
        onDismiss?.onDismiss(it)
        unregister()
        if (!supportsBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window?.decorView?.animateBlur(blurAmount, 1f, 50, true)?.start()
        }
    }
    setOnCancelListener {
        onCancel?.onCancel(it)
        unregister()
        if (!supportsBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window?.decorView?.animateBlur(blurAmount, 1f, 50, true)?.start()
        }
    }
}

fun TextView.setTextColorAlpha(alpha: Int) {
    setTextColor(ColorUtils.setAlphaComponent(currentTextColor, alpha))
}

fun View.updateGradiantBGRadius(
    ogRadius: Float,
    deviceRadius: Pair<Float, Float>,
    progress: Float,
    vararg updateOtherViews: View,
) {
    (background as? GradientDrawable)?.let { drawable ->
        val hasRail = resources.configuration.screenWidthDp >= 720
        val lerpL = MathUtils.lerp(
            ogRadius,
            if (hasRail && resources.isLTR) 0f else deviceRadius.first,
            max(0f, progress),
        )
        val lerpR = MathUtils.lerp(
            ogRadius,
            if (hasRail && !resources.isLTR) 0f else deviceRadius.second,
            max(0f, progress),
        )
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadii = floatArrayOf(lerpL, lerpL, lerpR, lerpR, 0f, 0f, 0f, 0f)
        background = drawable
        updateOtherViews.forEach {
            (it.background as? GradientDrawable)?.let { tDrawable ->
                tDrawable.shape = GradientDrawable.RECTANGLE
                tDrawable.cornerRadii = floatArrayOf(lerpL, lerpL, lerpR, lerpR, 0f, 0f, 0f, 0f)
            }
        }
    }
}

@RequiresApi(31)
fun View.animateBlur(
    @FloatRange(from = 0.1) from: Float,
    @FloatRange(from = 0.1) to: Float,
    duration: Long,
    removeBlurAtEnd: Boolean = false,
): ValueAnimator? {
    if (context.powerManager.isPowerSaveMode) {
        if (to <= 0.1f) {
            setRenderEffect(null)
        }
        return null
    }
    return ValueAnimator.ofFloat(from, to).apply {
        interpolator = FastOutLinearInInterpolator()
        this.duration = duration
        addUpdateListener { animator ->
            val amount = animator.animatedValue as Float
            try {
                setRenderEffect(
                    RenderEffect.createBlurEffect(amount, amount, Shader.TileMode.CLAMP),
                )
            } catch (_: Exception) {
            }
        }
        if (removeBlurAtEnd) {
            addListener(
                onEnd = {
                    setRenderEffect(null)
                },
            )
        }
    }
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
    val screen = Rect(
        0,
        0,
        Resources.getSystem().displayMetrics.widthPixels,
        Resources.getSystem().displayMetrics.heightPixels,
    )
    return actualPosition.intersect(screen)
}
