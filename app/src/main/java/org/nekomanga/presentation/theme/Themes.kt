package org.nekomanga.presentation.theme

import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import org.nekomanga.R

enum class Themes {
    Monet,
    Neko,
    Lavender,
    Lime,
    MangaDex,
    Outrun,
    SpringAndDusk,
    Strawberries,
    Tako,
    TealAndSapphire;

    val isDarkTheme = nightMode() == AppCompatDelegate.MODE_NIGHT_YES
    val followsSystem = nightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

    // With the way kotlin handles when(enum) and the fact that the main activity uses these methods
    // immediately, we were getting a .clone() error
    // to prevent that we are explictly calling this ==

    @StyleRes
    fun styleRes(): Int {
        return when {
            this == Monet -> R.style.Theme_Tachiyomi_Monet
            this == Outrun -> R.style.Theme_Tachiyomi_Outrun
            this == SpringAndDusk -> R.style.Theme_Tachiyomi_MidnightDusk
            this == MangaDex -> R.style.Theme_Tachiyomi_MangaDex
            this == TealAndSapphire -> R.style.Theme_Tachiyomi_SapphireDusk
            this == Lavender -> R.style.Theme_Tachiyomi_Lavender
            this == Strawberries -> R.style.Theme_Tachiyomi_Strawberries
            this == Tako -> R.style.Theme_Tachiyomi_Tako
            this == Lime -> R.style.Theme_Tachiyomi_FlatLime
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
            this == SpringAndDusk -> R.string.spring_blossom
            this == MangaDex -> R.string.mangadex
            this == TealAndSapphire -> R.string.teal_ocean
            this == Lavender -> R.string.lavender
            this == Strawberries -> R.string.strawberry_daiquiri
            this == Tako -> R.string.tako
            this == Lime -> R.string.lime_time
            else -> R.string.white_theme // Default
        }
    }

    @StringRes
    fun darkNameRes(): Int {
        return when {
            this == Monet -> R.string.a_calmer_you
            this == Neko -> R.string.white_theme
            this == SpringAndDusk -> R.string.midnight_dusk
            this == TealAndSapphire -> R.string.sapphire_dusk
            this == Lavender -> R.string.violet
            this == Strawberries -> R.string.chocolate_strawberries
            this == Lime -> R.string.flat_lime
            else -> this.nameRes()
        }
    }
}
