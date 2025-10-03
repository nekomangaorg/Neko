package org.nekomanga.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.nekomanga.R

object Typefaces {
    private val defaultTypography = Typography()
    val mplusRounded =
        FontFamily(
            Font(R.font.mplus_rounded1c_thin, FontWeight.Thin),
            Font(R.font.mplus_rounded1c_black, FontWeight.Black),
            Font(R.font.mplus_rounded1c_bold, FontWeight.Bold),
            Font(R.font.mplus_rounded1c_extra_bold, FontWeight.ExtraBold),
            Font(R.font.mplus_rounded1c_medium, FontWeight.Medium),
            Font(R.font.mplus_rounded1c_semi_bold, FontWeight.SemiBold),
            Font(R.font.mplus_rounded1c_regular, FontWeight.Normal),
        )

    val appTypography =
        Typography(
            displayLarge = defaultTypography.displayLarge.copy(fontFamily = mplusRounded),
            displayMedium = defaultTypography.displayMedium.copy(fontFamily = mplusRounded),
            displaySmall = defaultTypography.displaySmall.copy(fontFamily = mplusRounded),
            headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = mplusRounded),
            headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = mplusRounded),
            headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = mplusRounded),
            titleLarge = defaultTypography.titleLarge.copy(fontFamily = mplusRounded),
            titleMedium = defaultTypography.titleMedium.copy(fontFamily = mplusRounded),
            titleSmall = defaultTypography.titleSmall.copy(fontFamily = mplusRounded),
            bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = mplusRounded),
            bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = mplusRounded),
            bodySmall = defaultTypography.bodySmall.copy(fontFamily = mplusRounded),
            labelLarge = defaultTypography.labelLarge.copy(fontFamily = mplusRounded),
            labelMedium = defaultTypography.labelMedium.copy(fontFamily = mplusRounded),
            labelSmall = defaultTypography.labelSmall.copy(fontFamily = mplusRounded),
        )

    val LocalTypography = staticCompositionLocalOf { appTypography }
}
