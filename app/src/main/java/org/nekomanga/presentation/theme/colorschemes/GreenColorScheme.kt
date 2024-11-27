package org.nekomanga.presentation.theme.colorschemes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal object GreenColorScheme : BaseColorScheme() {
    override val lightScheme =
        lightColorScheme(
            primary = Color(0xFF57BD79),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF82D69E),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFF1DA750),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFF3BB368),
            onSecondaryContainer = Color(0xFFFFFFFF),
            tertiary = Color(0xFF0B893A),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFF1FA551),
            onTertiaryContainer = Color(0xFFFFFFFF),
            error = Color(0xFFC14033),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFDC6559),
            onErrorContainer = Color(0xFFFFFFFF),
            background = Color(0xFFE9EFEB),
            onBackground = Color(0xFFFFFFFF),
            surface = Color(0xFFE9EFEB),
            onSurface = Color(0xFF000000),
            surfaceVariant = Color(0xFFD9ECE0),
            onSurfaceVariant = Color(0xFF000000),
            outline = Color(0xFFBCE5C9),
            outlineVariant = Color(0xFFAEDFC0),
            scrim = Color(0xFFFFFFFF),
            inverseSurface = Color(0xFFFFFFFF),
            inverseOnSurface = Color(0xFFFFFFFF),
            inversePrimary = Color(0xFFAEDFC0),
            surfaceDim = Color(0xFFE9EFEB),
            surfaceBright = Color(0xFFE9EFEB),
            surfaceContainerLowest = Color(0xFFF0F4F1),
            surfaceContainerLow = Color(0xFFEDF2EF),
            surfaceContainer = Color(0xFFE9EFEB),
            surfaceContainerHigh = Color(0xFFD2D7D4),
            surfaceContainerHighest = Color(0xFFBABFBC),
        )

    override val darkScheme =
        darkColorScheme(
            primary = Color(0xFF7CF7A5),
            onPrimary = Color(0xFF043314),
            primaryContainer = Color(0xFFACFCC7),
            onPrimaryContainer = Color(0xFF043314),
            secondary = Color(0xFF4AF88A),
            onSecondary = Color(0xDE000000),
            secondaryContainer = Color(0xFF75FAA7),
            onSecondaryContainer = Color(0xDE000000),
            tertiary = Color(0xFF101820),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFF000000),
            onTertiaryContainer = Color(0xFF000000),
            error = Color(0xFFC14033),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFDC6559),
            onErrorContainer = Color(0xFFFFFFFF),
            background = Color(0xFF202125),
            onBackground = Color(0xFFFFFFFF),
            surface = Color(0xFF202125),
            onSurface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF292929),
            onSurfaceVariant = Color(0xFFFFFFFF),
            outline = Color(0xFFACFCC7),
            outlineVariant = Color(0xFFACFCC7),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFF000000),
            inverseOnSurface = Color(0xFF000000),
            inversePrimary = Color(0xFF000000),
            surfaceDim = Color(0xFF202125),
            surfaceBright = Color(0xFF202125),
            surfaceContainerLowest = Color(0xFF1A1A1E),
            surfaceContainerLow = Color(0xFF1D1E21),
            surfaceContainer = Color(0xFF202125),
            surfaceContainerHigh = Color(0xFF36373B),
            surfaceContainerHighest = Color(0xFF4D4D51),
        )
}
