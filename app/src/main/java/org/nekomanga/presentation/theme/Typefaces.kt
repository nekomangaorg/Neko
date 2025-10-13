package org.nekomanga.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.nekomanga.R

object Typefaces {
    private val defaultTypography = Typography()

    private const val LETTER_SPACING = -.15
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
            displayLarge =
                defaultTypography.displayLarge.copy(
                    fontFamily = mplusRounded,
                    letterSpacing = LETTER_SPACING.sp,
                ),
            displayMedium =
                defaultTypography.displayMedium.copy(
                    fontFamily = mplusRounded,
                    letterSpacing = LETTER_SPACING.sp,
                ),
            displaySmall =
                defaultTypography.displaySmall.copy(
                    fontFamily = mplusRounded,
                    letterSpacing = LETTER_SPACING.sp,
                ),
            headlineLarge =
                defaultTypography.headlineLarge.copy(
                    fontFamily = mplusRounded,
                    letterSpacing = LETTER_SPACING.sp,
                ),
            headlineMedium =
                defaultTypography.headlineMedium.copy(
                    fontFamily = mplusRounded,
                    letterSpacing = LETTER_SPACING.sp,
                ),
            headlineSmall =
                defaultTypography.headlineSmall.copy(
                    fontFamily = mplusRounded,
                    letterSpacing = LETTER_SPACING.sp,
                ),
            titleLarge =
                defaultTypography.titleLarge.copy(
                    fontFamily = mplusRounded,
                    letterSpacing = LETTER_SPACING.sp,
                ),
            titleMedium =
                defaultTypography.titleMedium.copy(
                    fontFamily = mplusRounded,
                    letterSpacing = LETTER_SPACING.sp,
                ),
            titleSmall =
                defaultTypography.titleSmall.copy(
                    fontFamily = mplusRounded,
                    letterSpacing = LETTER_SPACING.sp,
                ),
            bodyLarge =
                defaultTypography.bodyLarge.copy(
                    fontFamily = mplusRounded,
                    letterSpacing = LETTER_SPACING.sp,
                ),
            bodyMedium =
                defaultTypography.bodyMedium.copy(
                    fontFamily = mplusRounded,
                    letterSpacing = LETTER_SPACING.sp,
                ),
            bodySmall =
                defaultTypography.bodySmall.copy(
                    fontFamily = mplusRounded,
                    letterSpacing = LETTER_SPACING.sp,
                ),
            labelLarge =
                defaultTypography.labelLarge.copy(
                    fontFamily = mplusRounded,
                    letterSpacing = LETTER_SPACING.sp,
                ),
            labelMedium =
                defaultTypography.labelMedium.copy(
                    fontFamily = mplusRounded,
                    letterSpacing = LETTER_SPACING.sp,
                ),
            labelSmall =
                defaultTypography.labelSmall.copy(
                    fontFamily = mplusRounded,
                    letterSpacing = LETTER_SPACING.sp,
                ),
        )
}
