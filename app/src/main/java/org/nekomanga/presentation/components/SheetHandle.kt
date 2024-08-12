package org.nekomanga.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.theme.Size

@Composable
fun ColumnScope.sheetHandle() {
    Gap(16.dp)
    Box(
        modifier =
            Modifier.width(50.dp)
                .height(Size.tiny)
                .background(
                    color =
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = NekoColors.disabledAlphaLowContrast),
                    CircleShape)
                .align(Alignment.CenterHorizontally),
    )
}
