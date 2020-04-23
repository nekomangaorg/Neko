package eu.kanade.tachiyomi.util.system

import android.annotation.SuppressLint
import android.os.Build
import android.view.WindowInsets

@SuppressLint("NewApi")
fun WindowInsets.getBottomInsets(): Int {
    return when (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        true -> mandatorySystemGestureInsets.bottom
        false -> systemWindowInsetBottom
    }
}
