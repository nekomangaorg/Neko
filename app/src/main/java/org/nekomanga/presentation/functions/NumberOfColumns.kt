package org.nekomanga.presentation.functions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/** Calculates the number of columns from the raw value saved in the preferences */
@Composable
fun numberOfColumns(rawValue: Float, forText: Boolean = false, useHeight: Boolean = false): Int {
    // for whatever reason output number doesnt match unless we convert but the raw generates the
    // correct columns
    val value = if (forText) (rawValue / 2f) - .5f else rawValue
    val size = 1.5f.pow(value)
    val trueSize =
        AutofitRecyclerView.MULTIPLE * ((size * 100 / AutofitRecyclerView.MULTIPLE).roundToInt()) /
            100f

    val screenDimension =
        if (useHeight) LocalConfiguration.current.screenHeightDp
        else LocalConfiguration.current.screenWidthDp

    val dpDimension = (screenDimension / 100f).roundToInt()
    val math = (dpDimension / trueSize).roundToInt()
    return max(1, math)
}
