package eu.kanade.tachiyomi.util.system

import androidx.palette.graphics.Palette

fun Palette.getBestColor(): Int? {
    val vibPopulation = vibrantSwatch?.population ?: -1

    val domLum = dominantSwatch?.hsl?.get(2) ?: -1f

    val mutedPopulation = mutedSwatch?.population ?: -1
    val mutedSaturationLimit = if (mutedPopulation > vibPopulation * 3f) 0.1f else 0.25f

    return when {
        (dominantSwatch?.hsl?.get(1) ?: 0f) >= .25f &&
            domLum <= .8f && domLum > .2f -> {
            dominantSwatch?.rgb
        }
        vibPopulation >= mutedPopulation * 0.75f -> {
            vibrantSwatch?.rgb
        }
        mutedPopulation > vibPopulation * 1.5f &&
            (mutedSwatch?.hsl?.get(1) ?: 0f) > mutedSaturationLimit -> {
            mutedSwatch?.rgb
        }
        else -> {
            arrayListOf(vibrantSwatch, lightVibrantSwatch, darkVibrantSwatch).sortedBy {
                if (it === vibrantSwatch) {
                    (it?.population ?: -1) * 3
                } else {
                    it?.population ?: -1
                }
            }[1]?.rgb
        }
    }
}
