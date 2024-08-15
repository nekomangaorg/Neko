package org.nekomanga.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.theme.Size

/** custom chip so you can pass combinedClickable */
@Composable
fun Chip(
    label: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(Size.small)).then(modifier),
        tonalElevation = Size.small,
        shape = RoundedCornerShape(Size.small),
        color = containerColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier =
                Modifier.padding(vertical = 12.dp, horizontal = 12.dp)
                    .background(Color.Transparent),
            color =
                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = NekoColors.mediumAlphaHighContrast),
        )
    }
}
