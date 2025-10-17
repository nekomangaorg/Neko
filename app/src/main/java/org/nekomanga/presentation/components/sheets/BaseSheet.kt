package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.sheetHandle
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.theme.Shapes

@Composable
fun BaseSheet(
    themeColor: ThemeColorState,
    maxSheetHeightPercentage: Float = .7f,
    minSheetHeightPercentage: Float = 0f,
    topPaddingAroundContent: Dp = 16.dp,
    bottomPaddingAroundContent: Dp = 16.dp,
    showHandle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColor.rippleConfiguration,
        LocalTextSelectionColors provides themeColor.textSelectionColors,
    ) {
        val screenHeight = LocalConfiguration.current.screenHeightDp
        val maxSheetHeight = screenHeight * maxSheetHeightPercentage
        val minSheetHeight = screenHeight * minSheetHeightPercentage
        ElevatedCard(
            modifier =
                Modifier.fillMaxWidth().requiredHeightIn(minSheetHeight.dp, maxSheetHeight.dp),
            shape = RoundedCornerShape(topStart = Shapes.sheetRadius, topEnd = Shapes.sheetRadius),
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier.navigationBarsPadding().imePadding().verticalScroll(scrollState)
            ) {
                if (showHandle) {
                    sheetHandle()
                }

                Gap(topPaddingAroundContent)
                content()
                Gap(bottomPaddingAroundContent)
            }
        }
    }
}
