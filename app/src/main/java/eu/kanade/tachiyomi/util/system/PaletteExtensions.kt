package eu.kanade.tachiyomi.util.system

import androidx.palette.graphics.Palette

fun Palette.getBestColor(): Int? {
    val vibPopulation = vibrantSwatch?.population ?: -1
    val mutedPopulation = mutedSwatch?.population ?: -1

    // Helper for readability
    fun Palette.Swatch.isSaturatedAndMidTone(): Boolean {
        val saturation = hsl[1]
        val luminance = hsl[2]
        return saturation >= 0.25f && luminance in 0.2f..0.8f
    }

    // 1. Check Dominant
    dominantSwatch?.let { swatch -> if (swatch.isSaturatedAndMidTone()) return swatch.rgb }

    // 2. Check Vibrant
    if (vibPopulation >= mutedPopulation * 0.75f) {
        vibrantSwatch?.let {
            return it.rgb
        }
    }

    // 3. Check Muted
    val mutedSaturationLimit = if (mutedPopulation > vibPopulation * 3f) 0.1f else 0.25f
    if (
        mutedPopulation > vibPopulation * 1.5f &&
            (mutedSwatch?.hsl?.get(1) ?: 0f) > mutedSaturationLimit
    ) {
        mutedSwatch?.let {
            return it.rgb
        }
    }

    // 4. Safe Fallback: Filter nulls, sort descending by population, take top
    val fallbackSwatches = listOfNotNull(vibrantSwatch, lightVibrantSwatch, darkVibrantSwatch)
    return fallbackSwatches.maxByOrNull { it.population }?.rgb
}
