package org.nekomanga.presentation.functions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/** Calculates the number of columns from the raw value saved in the preferences */
@Composable
fun numberOfColumns(rawValue: Float): Int {
    val size = 1.5f.pow(rawValue)
    val trueSize =
        AutofitRecyclerView.MULTIPLE * ((size * 100 / AutofitRecyclerView.MULTIPLE).roundToInt()) /
            100f
    val dpWidth = (LocalConfiguration.current.screenWidthDp / 100f).roundToInt()
    val math = (dpWidth / trueSize).roundToInt()
    return max(1, math)
}
