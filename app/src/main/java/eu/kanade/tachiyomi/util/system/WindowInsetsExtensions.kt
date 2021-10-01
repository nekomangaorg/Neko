package eu.kanade.tachiyomi.util.system

import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.core.view.WindowInsetsCompat

fun WindowInsets.getBottomGestureInsets(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) mandatorySystemGestureInsets.bottom
    else systemWindowInsetBottom
}

/** returns if device using gesture nav and supports true edge to edge */
fun WindowInsets.isBottomTappable() = (
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        systemWindowInsetBottom != tappableElementInsets.bottom
    )

fun WindowInsets.hasSideInsets() = systemWindowInsetLeft > 0 || systemWindowInsetRight > 0

val View.rootWindowInsetsCompat
    get() = rootWindowInsets?.let { WindowInsetsCompat.toWindowInsetsCompat(it) }

/** returns if device is in landscape with 2/3 button mode */
fun WindowInsets.hasSideNavBar() =
    (systemWindowInsetLeft > 0 || systemWindowInsetRight > 0) && !isBottomTappable() &&
        systemWindowInsetBottom == 0

@RequiresApi(Build.VERSION_CODES.R)
fun WindowInsets.isImeVisible() = isVisible(WindowInsets.Type.ime())

fun WindowInsets.topCutoutInset() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    displayCutout?.safeInsetTop ?: 0
} else 0

fun WindowInsets.bottomCutoutInset() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    displayCutout?.safeInsetBottom ?: 0
} else 0
