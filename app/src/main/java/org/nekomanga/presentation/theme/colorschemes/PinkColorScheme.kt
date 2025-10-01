package org.nekomanga.presentation.theme.colorschemes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal object PinkColorScheme : BaseColorScheme() {
    // Generated using MaterialKolor Builder version 1.2.1 (103)
    // https://materialkolor.com/?color_seed=FFE570A0&dark_mode=false&color_spec=SPEC_2025&package_name=com.example.app&expressive=true

    override val lightScheme =
        lightColorScheme(
            primary = Color(0xFFA03967),
            onPrimary = Color(0xFFFFF7F7),
            primaryContainer = Color(0xFFFA81B2),
            onPrimaryContainer = Color(0xFF580030),
            inversePrimary = Color(0xFFFA81B2),
            secondary = Color(0xFF745761), // Unread badge
            onSecondary = Color(0xFFFFF7F7), // Unread badge text
            secondaryContainer =
                Color(0xFFFFD9E4), // Navigation bar selector pill & progress indicator (remaining)
            onSecondaryContainer = Color(0xFF664A53), // Navigation bar selector icon
            tertiary = Color(0xFF745479), // Downloaded badge
            onTertiary = Color(0xFFFFF7FB), // Downloaded badge text
            tertiaryContainer = Color(0xFFF6CDF9),
            onTertiaryContainer = Color(0xFF604265),
            background = Color(0xFFFFF8F8),
            onBackground = Color(0xFF3C2F33),
            surface = Color(0xFFFFF8F8),
            onSurface = Color(0xFF3C2F33),
            surfaceVariant = Color(0xFFF3DDE2), // Navigation bar background (ThemePrefWidget)
            onSurfaceVariant = Color(0xFF6B5B5F),
            surfaceTint = Color(0xFFA03967),
            inverseSurface = Color(0xFF130D0E),
            inverseOnSurface = Color(0xFFA69A9C),
            outline = Color(0xFF87767B),
            outlineVariant = Color(0xFFC1ADB2),
            scrim = Color(0xFF000000),
            surfaceBright = Color(0xFFFFF8F8),
            surfaceDim = Color(0xFFEAD5DA),
            error = Color(0xFFA8364B),
            onError = Color(0xFFFFF7F7),
            errorContainer = Color(0xFFF97386),
            onErrorContainer = Color(0xFF6E0523),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFFFF0F3),
            surfaceContainer = Color(0xFFFCE9ED), // Navigation bar background
            surfaceContainerHigh = Color(0xFFF7E3E8),
            surfaceContainerHighest = Color(0xFFF3DDE2),
            primaryFixed = Color(0xFFFA81B2),
            primaryFixedDim = Color(0xFFEA74A4),
            onPrimaryFixed = Color(0xFF21000F),
            onPrimaryFixedVariant = Color(0xFF67063A),
            secondaryFixed = Color(0xFFFFD9E4),
            secondaryFixedDim = Color(0xFFF0CBD6),
            onSecondaryFixed = Color(0xFF523841),
            onSecondaryFixedVariant = Color(0xFF70545D),
            tertiaryFixed = Color(0xFFF6CDF9),
            tertiaryFixedDim = Color(0xFFE7BFEA),
            onTertiaryFixed = Color(0xFF4C2F52),
            onTertiaryFixedVariant = Color(0xFF6B4B6F),
        )

    override val darkScheme =
        darkColorScheme(
            primary = Color(0xFFE570A0),
            onPrimary = Color(0xFF370318),
            primaryContainer = Color(0xFFF099BC),
            onPrimaryContainer = Color(0xFF370318),
            inversePrimary = Color(0xFF8D0C3F),
            secondary = Color(0xFFF02475),
            onSecondary = Color(0xFF370318),
            secondaryContainer = Color(0xFFF34C8F),
            onSecondaryContainer = Color(0xFF370318),
            tertiary = Color(0xFFA30042),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFCC0A58),
            onTertiaryContainer = Color(0xFFFFFFFF),
            background = Color(0xFF16151D),
            onBackground = Color(0xFFFFFFFF),
            surface = Color(0xFF16151D),
            onSurface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF272026),
            onSurfaceVariant = Color(0xFFFFFFFF),
            surfaceTint = Color(0xFFE570A0),
            inverseSurface = Color(0xFFFFFFFF),
            inverseOnSurface = Color(0xFF000000),
            error = Color(0xFFC14033),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFDC6559),
            onErrorContainer = Color(0xFFFFFFFF),
            outline = Color(0xFFF5C6D9),
            outlineVariant = Color(0xFFFAE2EC),
            scrim = Color(0xFF000000),
            surfaceBright = Color(0xFF000000),
            surfaceContainer = Color(0xFF16151D),
            surfaceContainerHigh = Color(0xFF2D2C34),
            surfaceContainerHighest = Color(0xFF45444A),
            surfaceContainerLow = Color(0xFF14131A),
            surfaceContainerLowest = Color(0xFF121117),
            surfaceDim = Color(0xFF000000),
            primaryFixed = Color(0xFFF099BC),
            primaryFixedDim = Color(0xFFCC6090),
            onPrimaryFixed = Color(0xFF370318),
            onPrimaryFixedVariant = Color(0xFFA30042),
            secondaryFixed = Color(0xFFF34C8F),
            secondaryFixedDim = Color(0xFFD84481),
            onSecondaryFixed = Color(0xFF370318),
            onSecondaryFixedVariant = Color(0xFFF02475),
            tertiaryFixed = Color(0xFFCC0A58),
            tertiaryFixedDim = Color(0xFF990842),
            onTertiaryFixed = Color(0xFFFFFFFF),
            onTertiaryFixedVariant = Color(0xFFA30042),
        )
}
