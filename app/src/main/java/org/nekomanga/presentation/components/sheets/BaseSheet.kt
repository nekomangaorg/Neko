package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun BaseSheet(
    themeColor: ThemeColorState,
    maxSheetHeightPercentage: Float = .7f,
    minSheetHeightPercentage: Float = 0f,
    bottomPaddingAroundContent: Dp = Size.medium,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColor.rippleConfiguration,
        LocalTextSelectionColors provides themeColor.textSelectionColors,
    ) {
        val screenHeight = LocalConfiguration.current.screenHeightDp
        val maxSheetHeight = screenHeight * maxSheetHeightPercentage
        val minSheetHeight = screenHeight * minSheetHeightPercentage

        val scrollState = rememberScrollState()

        Column(
            modifier =
                Modifier.navigationBarsPadding()
                    .imePadding()
                    .fillMaxWidth()
                    .requiredHeightIn(minSheetHeight.dp, maxSheetHeight.dp)
                    .verticalScroll(scrollState)
        ) {
            content()
            Gap(bottomPaddingAroundContent)
        }
    }
}
