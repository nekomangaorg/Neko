package eu.kanade.tachiyomi.util.system

import android.os.Build
import android.view.WindowInsets

fun WindowInsets.getBottomGestureInsets(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) mandatorySystemGestureInsets.bottom
    else systemWindowInsetBottom
}

/** returns if device using gesture nav and supports true edge to edge */
fun WindowInsets.isBottomTappable(): Boolean {
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        systemWindowInsetBottom != tappableElementInsets.bottom)
}

/** returns if device is in landscape with 2/3 button mode */
fun WindowInsets.hasSideNavBar(): Boolean = systemWindowInsetLeft > 0 || systemWindowInsetRight > 0
