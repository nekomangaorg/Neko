package org.nekomanga.presentation.screens.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.theme.Size

@Composable
fun RowScope.CategorySortButtons(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    categorySortClick: () -> Unit,
    sortString: String,
    isAscending: Boolean,
    textColor: Color = MaterialTheme.colorScheme.primary,
    categoryIsRefreshing: Boolean,
    categoryRefreshClick: () -> Unit,
) {
    TextButton(
        enabled = enabled,
        shapes =
            ButtonShapes(
                shape = ButtonGroupDefaults.connectedLeadingButtonShape,
                pressedShape = ButtonGroupDefaults.connectedLeadingButtonPressShape,
            ),
        onClick = categorySortClick,
    ) {
        Text(text = sortString, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
        Gap(Size.extraTiny)
        Icon(
            imageVector =
                when {
                    isAscending -> Icons.Default.ArrowDownward
                    else -> Icons.Default.ArrowUpward
                },
            contentDescription = null,
            modifier = Modifier.size(Size.large),
        )
    }
    Gap(ButtonGroupDefaults.ConnectedSpaceBetween)

    AnimatedContent(targetState = categoryIsRefreshing) { targetState ->
        when (targetState) {
            true -> {
                val strokeWidth = with(LocalDensity.current) { Size.tiny.toPx() }
                val stroke =
                    remember(strokeWidth) { Stroke(width = strokeWidth, cap = StrokeCap.Round) }
                IconButton(
                    enabled = false,
                    shapes =
                        IconButtonShapes(
                            shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                            pressedShape = ButtonGroupDefaults.connectedTrailingButtonPressShape,
                        ),
                    colors = IconButtonDefaults.iconButtonColors(disabledContentColor = textColor),
                    onClick = {},
                ) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(Size.large),
                        trackStroke = stroke,
                        stroke = stroke,
                    )
                }
            }

            false -> {

                IconButton(
                    enabled = enabled,
                    shapes =
                        IconButtonShapes(
                            shape = ButtonGroupDefaults.connectedTrailingButtonShape,
                            pressedShape = ButtonGroupDefaults.connectedTrailingButtonPressShape,
                        ),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = textColor),
                    onClick = categoryRefreshClick,
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(Size.large),
                    )
                }
            }
        }
    }
}
