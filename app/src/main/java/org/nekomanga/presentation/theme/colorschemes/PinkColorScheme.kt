package org.nekomanga.presentation.theme.colorschemes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Shades for hex colors https://noeldelgado.github.io/shadowlord/
// Color palette https://paletton.com/

internal object PinkColorScheme : BaseColorScheme() {
    override val lightScheme =
        lightColorScheme(
            primary = Color(0xFFA149BF),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFBA6FD5),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFFC43C97),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFD55EAE),
            onSecondaryContainer = Color(0xFFFFFFFF),
            tertiary = Color(0xFFEFE3F3),
            onTertiary = Color(0xFF000000),
            tertiaryContainer = Color(0xFFFDFBFE),
            onTertiaryContainer = Color(0xFF000000),
            error = Color(0xFFC14033),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFDC6559),
            onErrorContainer = Color(0xFFFFFFFF),
            background = Color(0xFFF7F4F8),
            onBackground = Color(0xDE240728),
            surface = Color(0xFFF7F4F8),
            onSurface = Color(0xDE240728),
            surfaceVariant = Color(0xFFF7F4F8),
            onSurfaceVariant = Color(0xFF000000),
            outline = Color(0xFFD55EAE),
            outlineVariant = Color(0xFFD55EAE),
            scrim = Color(0xFFFFFFFF),
            inverseSurface = Color(0xFFFFFFFF),
            inverseOnSurface = Color(0xFFFFFFFF),
            inversePrimary = Color(0xFFCD95E0),
            surfaceDim = Color(0xFFFFFFFF),
            surfaceBright = Color(0xFFFFFFFF),
            surfaceContainerLowest = Color(0xFFF9F6F9),
            surfaceContainerLow = Color(0xFFF8F5F9),
            surfaceContainer = Color(0xFFF7F4F8),
            surfaceContainerHigh = Color(0xFFDEDCDF),
            surfaceContainerHighest = Color(0xFFC6C3C6),
        )

    override val darkScheme =
        darkColorScheme(
            primary = Color(0xFFE570A0),
            onPrimary = Color(0xFF370318),
            primaryContainer = Color(0xFFF099BC),
            onPrimaryContainer = Color(0xFF370318),
            secondary = Color(0xFFF02475),
            onSecondary = Color(0xFF370318),
            secondaryContainer = Color(0xFFF34C8F),
            onSecondaryContainer = Color(0xFF370318),
            tertiary = Color(0xFF272026),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFF524450),
            onTertiaryContainer = Color(0xFFFFFFFF),
            error = Color(0xFFC14033),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFDC6559),
            onErrorContainer = Color(0xFFFFFFFF),
            background = Color(0xFF16151D),
            onBackground = Color(0xFFFFFFFF),
            surface = Color(0xFF16151D),
            onSurface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF16151D),
            onSurfaceVariant = Color(0xFFFFFFFF),
            outline = Color(0xFFF9C3D9),
            outlineVariant = Color(0xFFF9C3D9),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFF000000),
            inverseOnSurface = Color(0xFF000000),
            inversePrimary = Color(0xFF8D0C3F),
            surfaceDim = Color(0xFF000000),
            surfaceBright = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFF121117),
            surfaceContainerLow = Color(0xFF14131A),
            surfaceContainer = Color(0xFF16151D),
            surfaceContainerHigh = Color(0xFF2D2C34),
            surfaceContainerHighest = Color(0xFF45444A),
        )
}
