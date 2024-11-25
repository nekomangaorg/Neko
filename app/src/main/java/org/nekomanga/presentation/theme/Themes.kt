package org.nekomanga.presentation.theme

import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import org.nekomanga.R

enum class Themes {
    Monet,
    Neko,
    Purple,
    Green,
    Orange,
    Outrun,
    Pink,
    Red,
    Tako,
    BlueGreen;

    fun isDarkTheme() = nightMode() == AppCompatDelegate.MODE_NIGHT_YES

    fun followsSystem() = nightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

    // With the way kotlin handles when(enum) and the fact that the main activity uses these methods
    // immediately, we were getting a .clone() error
    // to prevent that we are explictly calling this ==

    @StyleRes
    fun styleRes(): Int {
        return when {
            this == Monet -> R.style.Theme_Tachiyomi_Monet
            this == Outrun -> R.style.Theme_Tachiyomi_Outrun
            this == Pink -> R.style.Theme_Tachiyomi_MidnightDusk
            this == Orange -> R.style.Theme_Tachiyomi_MangaDex
            this == BlueGreen -> R.style.Theme_Tachiyomi_SapphireDusk
            this == Purple -> R.style.Theme_Tachiyomi_Lavender
            this == Red -> R.style.Theme_Tachiyomi_Strawberries
            this == Tako -> R.style.Theme_Tachiyomi_Tako
            this == Green -> R.style.Theme_Tachiyomi_FlatLime
            else -> R.style.Theme_Tachiyomi // DEFAULT
        }
    }

    fun nightMode(): Int {
        return when {
            this == Outrun -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }

    @StringRes
    fun nameRes(): Int {
        return when {
            this == Monet -> R.string.a_brighter_you
            this == Outrun -> R.string.outrun
            this == Pink -> R.string.spring_blossom
            this == Orange -> R.string.orange_juice
            this == BlueGreen -> R.string.teal_ocean
            this == Purple -> R.string.lavender
            this == Red -> R.string.strawberry_daiquiri
            this == Tako -> R.string.tako
            this == Green -> R.string.lime_time
            else -> R.string.white_theme // Default
        }
    }

    @StringRes
    fun darkNameRes(): Int {
        return when {
            this == Monet -> R.string.a_calmer_you
            this == Neko -> R.string.white_theme
            this == Pink -> R.string.midnight_dusk
            this == BlueGreen -> R.string.sapphire_dusk
            this == Purple -> R.string.violet
            this == Red -> R.string.chocolate_strawberries
            this == Green -> R.string.flat_lime
            else -> this.nameRes()
        }
    }
}
