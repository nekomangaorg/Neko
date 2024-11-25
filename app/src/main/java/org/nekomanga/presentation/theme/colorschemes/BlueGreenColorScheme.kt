package org.nekomanga.presentation.theme.colorschemes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Shades for hex colors https://noeldelgado.github.io/shadowlord/
// Color palette https://paletton.com/

internal object BlueGreenColorScheme : BaseColorScheme() {
    override val lightScheme =
        lightColorScheme(
            primary = Color(0xFF5F9C96),
            onPrimary = Color(0xFF000000),
            primaryContainer = Color(0xFF92C1BC),
            onPrimaryContainer = Color(0xFF000000),
            secondary = Color(0xFF05B1A4),
            onSecondary = Color(0xFF000000),
            secondaryContainer = Color(0xFF2AB8AE),
            onSecondaryContainer = Color(0xFF000000),
            tertiary = Color(0xFF008B81),
            onTertiary = Color(0xFF000000),
            tertiaryContainer = Color(0xFF1A9C93),
            onTertiaryContainer = Color(0xFFFFFFFF),
            error = Color(0xFFC14033),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFDC6559),
            onErrorContainer = Color(0xFFFFFFFF),
            background = Color(0xFFF0F7F7),
            onBackground = Color(0xFF000000),
            surface = Color(0xFFF0F7F7),
            onSurface = Color(0xFF000000),
            surfaceVariant = Color(0xFFF0F7F7),
            onSurfaceVariant = Color(0xFF000000),
            outline = Color(0xFFBFD7D5),
            outlineVariant = Color(0xFFDFEBEA),
            scrim = Color(0xFFFFFFFF),
            inverseSurface = Color(0xFFFFFFFF),
            inverseOnSurface = Color(0xFFFFFFFF),
            inversePrimary = Color(0xFFA1E1DB),
            surfaceDim = Color(0xFFFFFFFF),
            surfaceBright = Color(0xFFFFFFFF),
            surfaceContainerLowest = Color(0xFFF3F9F9),
            surfaceContainerLow = Color(0xFFF2F8F8),
            surfaceContainer = Color(0xFFF0F7F7),
            surfaceContainerHigh = Color(0xFFD8DEDE),
            surfaceContainerHighest = Color(0xFFC0C6C6),
        )

    override val darkScheme =
        darkColorScheme(
            primary = Color(0xFF80B8D1),
            onPrimary = Color(0xFF02212C),
            primaryContainer = Color(0xFFB0D8E9),
            onPrimaryContainer = Color(0xFF02212C),
            secondary = Color(0xFF589AB8),
            onSecondary = Color(0xFF02212C),
            secondaryContainer = Color(0xFF83BCD5),
            onSecondaryContainer = Color(0xFF02212C),
            tertiary = Color(0xFF1F6C8D),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFF387B98),
            onTertiaryContainer = Color(0xFFFFFFFF),
            error = Color(0xFFC14033),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFDC6559),
            onErrorContainer = Color(0xFFFFFFFF),
            background = Color(0xFF14191B),
            onBackground = Color(0xFFFFFFFF),
            surface = Color(0xFF14191B),
            onSurface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF14191B),
            onSurfaceVariant = Color(0xFFFFFFFF),
            outline = Color(0xFFCCE3ED),
            outlineVariant = Color(0xFFE6F1F6),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFF000000),
            inverseOnSurface = Color(0xFF000000),
            inversePrimary = Color(0xFF1D698B),
            surfaceDim = Color(0xFF000000),
            surfaceBright = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFF101416),
            surfaceContainerLow = Color(0xFF121718),
            surfaceContainer = Color(0xFF14191B),
            surfaceContainerHigh = Color(0xFF2B3032),
            surfaceContainerHighest = Color(0xFF434749),
        )
}
