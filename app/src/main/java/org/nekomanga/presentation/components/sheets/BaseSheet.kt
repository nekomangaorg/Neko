package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.ThemeColors
import org.nekomanga.presentation.theme.Shapes

@Composable
fun BaseSheet(themeColors: ThemeColors, maxSheetHeightPercentage: Float = .7f, content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(LocalRippleTheme provides themeColors.rippleTheme, LocalTextSelectionColors provides themeColors.textSelectionColors) {

        val maxSheetHeight = LocalConfiguration.current.screenHeightDp * maxSheetHeightPercentage
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .requiredHeightIn(0.dp, maxSheetHeight.dp),
            shape = RoundedCornerShape(Shapes.sheetRadius),
        ) {
            Gap(16.dp)
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(4.dp)
                    .background(color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaLowContrast), CircleShape)
                    .align(Alignment.CenterHorizontally),
            )

            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Gap(24.dp)
                content()
                Gap(16.dp)
            }
        }

    }
}
