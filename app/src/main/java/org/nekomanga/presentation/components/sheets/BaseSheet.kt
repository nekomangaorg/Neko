package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColors
import org.nekomanga.presentation.theme.Shapes

@Composable
fun BaseSheet(themeColor: ThemeColors, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRippleTheme provides themeColor.rippleTheme) {

        val maxSheetHeight = LocalConfiguration.current.screenHeightDp * .7
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeightIn(0.dp, maxSheetHeight.dp),
            shape = RoundedCornerShape(Shapes.sheetRadius),
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding(),
            ) {
                Gap(16.dp)
                content()
                Gap(16.dp)
            }
        }

    }
}
