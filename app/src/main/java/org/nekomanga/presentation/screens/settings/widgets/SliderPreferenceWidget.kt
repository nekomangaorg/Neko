package org.nekomanga.presentation.screens.settings.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.settings.BasePreferenceWidget
import org.nekomanga.presentation.screens.settings.PrefsHorizontalPadding
import org.nekomanga.presentation.theme.Size

@Composable
fun SliderPreferenceWidget(
    modifier: Modifier = Modifier,
    value: Int,
    min: Int = 0,
    max: Int = 100,
    steps: Int = 0,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    valueFormatter: (Int) -> String = { "$it" },
    onValueChange: (Int) -> Unit,
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }

    BasePreferenceWidget(
        modifier = modifier,
        title = null,
        icon =
            if (icon != null) {
                { Icon(imageVector = icon, tint = iconTint, contentDescription = null) }
            } else {
                null
            },
        subcomponent = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = PrefsHorizontalPadding)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = title,
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal
                            ),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = valueFormatter(sliderValue.roundToInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        modifier = Modifier.alpha(NekoColors.mediumAlphaLowContrast),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 10,
                    )
                }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onValueChange(sliderValue.roundToInt()) },
                    valueRange = min.toFloat()..max.toFloat(),
                    steps = steps,
                    colors =
                        SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor =
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                            thumbColor = MaterialTheme.colorScheme.primary,
                        ),
                    modifier = Modifier.fillMaxWidth().padding(top = Size.tiny),
                )
            }
        },
    )
}
