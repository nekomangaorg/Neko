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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Shapes

@Composable
fun BaseSheet(
    themeColor: ThemeColorState,
    maxSheetHeightPercentage: Float = .7f,
    minSheetHeightPercentage: Float = 0f,
    paddingAroundContent: Dp = 16.dp,
    showHandle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalRippleTheme provides themeColor.rippleTheme, LocalTextSelectionColors provides themeColor.textSelectionColors) {
        val screenHeight = LocalConfiguration.current.screenHeightDp
        val maxSheetHeight = screenHeight * maxSheetHeightPercentage
        val minSheetHeight = screenHeight * minSheetHeightPercentage
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .requiredHeightIn(minSheetHeight.dp, maxSheetHeight.dp),
            shape = RoundedCornerShape(Shapes.sheetRadius),
        ) {
            if (showHandle) {
                Gap(16.dp)
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(4.dp)
                        .background(color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaLowContrast), CircleShape)
                        .align(Alignment.CenterHorizontally),
                )
            }

            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Gap(paddingAroundContent)
                content()
                Gap(paddingAroundContent)
            }
        }
    }
}
