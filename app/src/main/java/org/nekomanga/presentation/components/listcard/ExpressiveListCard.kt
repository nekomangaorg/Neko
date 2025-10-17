package org.nekomanga.presentation.components.listcard

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.extensions.surfaceColorAtElevationCustomColor
import org.nekomanga.presentation.theme.Size

enum class ListCardType {
    Top,
    Center,
    Bottom,
    Single,
}

@Composable
fun ExpressiveListCard(
    modifier: Modifier = Modifier,
    listCardType: ListCardType,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    content: @Composable () -> Unit,
) {
    val defaultColors = CardDefaults.elevatedCardColors()
    val defaultThemeColorState = defaultThemeColorState()
    val elevatedColor =
        MaterialTheme.colorScheme.surfaceColorAtElevationCustomColor(
            themeColorState.containerColor,
            Size.small,
        )
    val colors =
        remember(themeColorState) {
            if (themeColorState == defaultThemeColorState) {
                defaultColors
            } else {
                defaultColors.copy(containerColor = elevatedColor)
            }
        }

    val shape =
        remember(listCardType) {
            when (listCardType) {
                ListCardType.Top ->
                    RoundedCornerShape(
                        topStart = Size.medium,
                        topEnd = Size.medium,
                        bottomEnd = Size.tiny,
                        bottomStart = Size.tiny,
                    )
                ListCardType.Center ->
                    RoundedCornerShape(
                        topStart = Size.tiny,
                        topEnd = Size.tiny,
                        bottomEnd = Size.tiny,
                        bottomStart = Size.tiny,
                    )
                ListCardType.Single -> {
                    RoundedCornerShape(Size.medium)
                }
                ListCardType.Bottom ->
                    RoundedCornerShape(
                        topStart = Size.tiny,
                        topEnd = Size.tiny,
                        bottomEnd = Size.medium,
                        bottomStart = Size.medium,
                    )
            }
        }

    ElevatedCard(modifier = modifier, shape = shape, colors = colors) { content() }
}
