package org.nekomanga.presentation.theme.colorschemes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Shades for hex colors https://noeldelgado.github.io/shadowlord/
// Color palette https://paletton.com/

internal object OrangeColorScheme : BaseColorScheme() {
    override val lightScheme =
        lightColorScheme(
            primary = Color(0xFFFF6740),
            onPrimary = Color(0xFF000000),
            primaryContainer = Color(0xFFFF8463),
            onPrimaryContainer = Color(0xFF000000),
            secondary = Color(0xFFFF6740),
            onSecondary = Color(0xFF000000),
            secondaryContainer = Color(0xFFFF8463),
            onSecondaryContainer = Color(0xFF000000),
            tertiary = Color(0xFFFF6840),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFF8463),
            onTertiaryContainer = Color(0xFFFFFFFF),
            error = Color(0xFFC14033),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFDC6559),
            onErrorContainer = Color(0xFFFFFFFF),
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF000000),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF000000),
            surfaceVariant = Color(0xFFFFFFFF),
            onSurfaceVariant = Color(0xFF000000),
            outline = Color(0xFFFFC2B3),
            outlineVariant = Color(0xFFFFE1D9),
            scrim = Color(0xFFFFFFFF),
            inverseSurface = Color(0xFFFFFFFF),
            inverseOnSurface = Color(0xFFFFFFFF),
            inversePrimary = Color(0xFFFFFFFF),
            surfaceDim = Color(0xFFFFFFFF),
            surfaceBright = Color(0xFFFFFFFF),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFFFFFFF),
            surfaceContainer = Color(0xFFFFFFFF),
            surfaceContainerHigh = Color(0xFFE6E6E6),
            surfaceContainerHighest = Color(0xFFCCCCCC),
        )

    override val darkScheme =
        darkColorScheme(
            primary = lightScheme.primary,
            onPrimary = lightScheme.onPrimary,
            primaryContainer = lightScheme.primaryContainer,
            onPrimaryContainer = lightScheme.onPrimaryContainer,
            secondary = lightScheme.secondary,
            onSecondary = lightScheme.onSecondary,
            secondaryContainer = lightScheme.secondaryContainer,
            onSecondaryContainer = lightScheme.onSecondaryContainer,
            tertiary = lightScheme.tertiary,
            onTertiary = lightScheme.onTertiary,
            tertiaryContainer = lightScheme.tertiaryContainer,
            onTertiaryContainer = lightScheme.onTertiaryContainer,
            error = lightScheme.error,
            onError = lightScheme.onError,
            errorContainer = lightScheme.errorContainer,
            onErrorContainer = lightScheme.onErrorContainer,
            background = Color(0xFF292929),
            onBackground = Color(0xFFFFFFFF),
            surface = Color(0xFF292929),
            onSurface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF292929),
            onSurfaceVariant = Color(0xFFFFFFFF),
            outline = lightScheme.outline,
            outlineVariant = lightScheme.outlineVariant,
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFF000000),
            inverseOnSurface = Color(0xFF000000),
            inversePrimary = Color(0xFF000000),
            surfaceDim = Color(0xFF292929),
            surfaceBright = Color(0xFF292929),
            surfaceContainerLowest = Color(0xFF212121),
            surfaceContainerLow = Color(0xFF252525),
            surfaceContainer = Color(0xFF292929),
            surfaceContainerHigh = Color(0xFF3E3E3E),
            surfaceContainerHighest = Color(0xFF545454),
        )
}
