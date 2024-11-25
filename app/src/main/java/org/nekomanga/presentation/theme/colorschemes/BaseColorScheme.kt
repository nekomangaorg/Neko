package org.nekomanga.presentation.theme.colorschemes

import androidx.compose.material3.ColorScheme

internal abstract class BaseColorScheme {

    abstract val darkScheme: ColorScheme
    abstract val lightScheme: ColorScheme

    fun getColorScheme(isDark: Boolean): ColorScheme {
        return (if (isDark) darkScheme else lightScheme)
    }
}
