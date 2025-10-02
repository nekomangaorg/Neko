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
    Pink,
    Retro,
    Blue,
    Brown,
    Tako,
    Teal,
    Nord,
    Monochrome;

    fun isDarkTheme() = nightMode() == AppCompatDelegate.MODE_NIGHT_YES

    fun followsSystem() = nightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

    // With the way kotlin handles when(enum) and the fact that the main activity uses these methods
    // immediately, we were getting a .clone() error
    // to prevent that we are explictly calling this ==

    @StyleRes
    fun styleRes(): Int {
        return when {
            this == Monet -> R.style.Theme_Neko_Monet
            this == Retro -> R.style.Theme_Neko_Retro
            this == Pink -> R.style.Theme_Neko_Pink
            this == Orange -> R.style.Theme_Neko_Orange
            this == Teal -> R.style.Theme_Neko_Teal
            this == Purple -> R.style.Theme_Neko_Purple
            this == Brown -> R.style.Theme_Neko_Brown
            this == Tako -> R.style.Theme_Neko_Tako
            this == Nord -> R.style.Theme_Neko_Nord
            this == Green -> R.style.Theme_Neko_Green
            this == Blue -> R.style.Theme_Neko_Blue
            this == Monochrome -> R.style.Theme_Neko_Monochrome
            else -> R.style.Theme_Neko // DEFAULT
        }
    }

    fun nightMode(): Int {
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    @StringRes
    fun nameRes(): Int {
        return when {
            this == Monet -> R.string.dynamic
            this == Retro -> R.string.retro
            this == Pink -> R.string.spring_blossom
            this == Orange -> R.string.orange_juice
            this == Teal -> R.string.teal_ocean
            this == Purple -> R.string.royal
            this == Brown -> R.string.chocolate
            this == Tako -> R.string.tako
            this == Nord -> R.string.nord
            this == Green -> R.string.jungle
            this == Blue -> R.string.crayon_blue
            this == Monochrome -> R.string.monochrome
            else -> R.string.app_name // Default
        }
    }
}
