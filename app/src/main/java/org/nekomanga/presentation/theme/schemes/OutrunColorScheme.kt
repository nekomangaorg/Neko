package org.nekomanga.presentation.theme.schemes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal object OutrunColorScheme : BaseColorScheme() {
    override val darkScheme =
        darkColorScheme(
            primary = Color(0xFFFFB1C5),
            onPrimary = Color(0xFF59192F),
            primaryContainer = Color(0xFFAC5C73),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFFFFB2BB),
            onSecondary = Color(0xFF670021),
            secondaryContainer = Color(0xFFC94560),
            onSecondaryContainer = Color(0xFFFFFFFF),
            tertiary = Color(0xFFCEBDFD),
            onTertiary = Color(0xFF35275D),
            tertiaryContainer = Color(0xFF190840),
            onTertiaryContainer = Color(0xFFA898D6),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = Color(0xFF181213),
            onBackground = Color(0xFFEEDFE1),
            surface = Color(0xFF141316),
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = Color(0xFF534346),
            onSurfaceVariant = Color(0xFFD8C1C5),
            outline = Color(0xFFA08C90),
            outlineVariant = Color(0xFF534346),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFFE6E1E5),
            inverseOnSurface = Color(0xFF313033),
            inversePrimary = Color(0xFF92465D),
            surfaceDim = Color(0xFF141316),
            surfaceBright = Color(0xFF3A383C),
            surfaceContainerLowest = Color(0xFF0F0E10),
            surfaceContainerLow = Color(0xFF1C1B1E),
            surfaceContainer = Color(0xFF201F22),
            surfaceContainerHigh = Color(0xFF2B292C),
            surfaceContainerHighest = Color(0xFF363437),
        )
    override val lightScheme =
        lightColorScheme(
            primary = Color(0xFF80384F),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFAC5C73),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFF981F3D),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFC94560),
            onSecondaryContainer = Color(0xFFFFFFFF),
            tertiary = Color(0xFF070021),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFF2D1F54),
            onTertiaryContainer = Color(0xFFBCABEA),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = Color(0xFFFFF8F8),
            onBackground = Color(0xFF211A1B),
            surface = Color(0xFFFDF8FC),
            onSurface = Color(0xFF1C1B1E),
            surfaceVariant = Color(0xFFF5DDE1),
            onSurfaceVariant = Color(0xFF534346),
            outline = Color(0xFF857276),
            outlineVariant = Color(0xFFD8C1C5),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFF313033),
            inverseOnSurface = Color(0xFFF4EFF3),
            inversePrimary = Color(0xFFFFB1C5),
            surfaceDim = Color(0xFFDDD9DD),
            surfaceBright = Color(0xFFFDF8FC),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF7F2F6),
            surfaceContainer = Color(0xFFF1ECF0),
            surfaceContainerHigh = Color(0xFFECE7EB),
            surfaceContainerHighest = Color(0xFFE6E1E5),
        )
}
