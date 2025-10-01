package org.nekomanga.presentation.theme.colorschemes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal object GreenColorScheme : BaseColorScheme() {
    override val lightScheme =
        lightColorScheme(
            primary = Color(0xFF386A00),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFB2F449),
            onPrimaryContainer = Color(0xFF092000),
            inversePrimary = Color(0xFFA6F26D),
            secondary = Color(0xFF57634E),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFDAE8CD),
            onSecondaryContainer = Color(0xFF151F0F),
            tertiary = Color(0xFF386665),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFBBECEC),
            onTertiaryContainer = Color(0xFF00201F),
            background = Color(0xFFFCFEF7),
            onBackground = Color(0xFF1A1C18),
            surface = Color(0xFFFCFEF7),
            onSurface = Color(0xFF1A1C18),
            surfaceVariant = Color(0xFFE0E4D5),
            onSurfaceVariant = Color(0xFF43483E),
            surfaceTint = Color(0xFF386A00),
            inverseSurface = Color(0xFF2F312B),
            inverseOnSurface = Color(0xFFF1F1EB),
            outline = Color(0xFF75796E),
            outlineVariant = Color(0xFFC4C8BA),
            scrim = Color(0xFF000000),
            surfaceBright = Color(0xFFFCFEF7),
            surfaceDim = Color(0xFFDCDFE4),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF7F9F1),
            surfaceContainer = Color(0xFFF1F4ED),
            surfaceContainerHigh = Color(0xFFECEFE8),
            surfaceContainerHighest = Color(0xFFE6EAE2),
            primaryFixed = Color(0xFFB2F449),
            primaryFixedDim = Color(0xFF96D735),
            onPrimaryFixed = Color(0xFF092000),
            onPrimaryFixedVariant = Color(0xFF225100),
            secondaryFixed = Color(0xFFDAE8CD),
            secondaryFixedDim = Color(0xFFBED2B3),
            onSecondaryFixed = Color(0xFF151F0F),
            onSecondaryFixedVariant = Color(0xFF404A39),
            tertiaryFixed = Color(0xFFBBECEC),
            tertiaryFixedDim = Color(0xFF9FD0CE),
            onTertiaryFixed = Color(0xFF00201F),
            onTertiaryFixedVariant = Color(0xFF1F4D4C),
        )

    override val darkScheme =
        darkColorScheme(
            primary = Color(0xFFA6F26D), // Bright Green
            onPrimary = Color(0xFF1E3700),
            primaryContainer = Color(0xFF2C5000), // Dark Green
            onPrimaryContainer = Color(0xFFB2F449),
            inversePrimary = Color(0xFF386A00),
            secondary = Color(0xFFC4CBBE), // Light Grey-Green
            onSecondary = Color(0xFF293423),
            secondaryContainer = Color(0xFF5E4941), // Rich Brown/Olive Container
            onSecondaryContainer = Color(0xFFDAE8CD),
            tertiary = Color(0xFF9FD0CE),
            onTertiary = Color(0xFF003736),
            tertiaryContainer = Color(0xFF384F4B),
            onTertiaryContainer = Color(0xFFBBECEC),
            background = Color(0xFF1D1513), // Deep Brown
            onBackground = Color(0xFFEBE0DD),
            surface = Color(0xFF1D1513), // Deep Brown
            onSurface = Color(0xFFEBE0DD),
            surfaceVariant = Color(0xFF4A3832), // Rich Mid Brown
            onSurfaceVariant = Color(0xFFD0C4C1),
            surfaceTint = Color(0xFFA6F26D),
            inverseSurface = Color(0xFFEBE0DD),
            inverseOnSurface = Color(0xFF1D1513),
            outline = Color(0xFF9C8F8B),
            outlineVariant = Color(0xFF4A3832),
            scrim = Color(0xFF000000),
            surfaceBright = Color(0xFF4A3832),
            surfaceDim = Color(0xFF1D1513),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),

            // Surface containers set to brown gradient
            surfaceContainerLowest = Color(0xFF110D0B),
            surfaceContainerLow = Color(0xFF241B19),
            surfaceContainer = Color(0xFF2B211E),
            surfaceContainerHigh = Color(0xFF372C28),
            surfaceContainerHighest = Color(0xFF453934),
            primaryFixed = Color(0xFFB2F449),
            primaryFixedDim = Color(0xFF96D735),
            onPrimaryFixed = Color(0xFF092000),
            onPrimaryFixedVariant = Color(0xFF225100),
            secondaryFixed = Color(0xFFDAE8CD),
            secondaryFixedDim = Color(0xFFBED2B3),
            onSecondaryFixed = Color(0xFF151F0F),
            onSecondaryFixedVariant = Color(0xFF404A39),
            tertiaryFixed = Color(0xFFBBECEC),
            tertiaryFixedDim = Color(0xFF9FD0CE),
            onTertiaryFixed = Color(0xFF00201F),
            onTertiaryFixedVariant = Color(0xFF1F4D4C),
        )
}
