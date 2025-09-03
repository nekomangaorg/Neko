package org.nekomanga.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.nekomanga.presentation.components.Outline as NekoOutline
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.theme.Size as NekoSize

@Composable
internal fun DownloadUnreadBadge(
    outline: Boolean,
    showUnread: Boolean,
    unreadCount: Int,
    showDownloads: Boolean,
    downloadCount: Int,
    offset: Dp = (-2).dp,
) {
    if (showUnread && unreadCount > 0 && (!showDownloads || downloadCount == 0)) {
        SoloBadge(
            outline = outline,
            offset = offset,
            backgroundColor = MaterialTheme.colorScheme.secondary,
            count = unreadCount.toString(),
            countColor = MaterialTheme.colorScheme.onSecondary,
        )
    } else if (showDownloads && downloadCount > 0 && (!showUnread || unreadCount == 0)) {
        SoloBadge(
            outline = outline,
            offset = offset,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            count = downloadCount.toString(),
            countColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        DoubleBadge(
            outline = outline,
            offset = offset,
            backgroundColor1 = MaterialTheme.colorScheme.surfaceVariant,
            backgroundColor2 = MaterialTheme.colorScheme.secondary,
            count1 = downloadCount.toString(),
            count1Color = MaterialTheme.colorScheme.onSurfaceVariant,
            count2 = unreadCount.toString(),
            count2Color = MaterialTheme.colorScheme.onSecondary,
        )
    }
}

@Composable
private fun SoloBadge(
    outline: Boolean,
    offset: Dp,
    backgroundColor: Color,
    count: String,
    countColor: Color,
) {
    Box(
        modifier =
            Modifier.offset(x = offset, y = offset)
                .background(backgroundColor, singleBadgeShape)
                .conditional(outline) {
                    this.border(
                        width = NekoOutline.thickness,
                        color = NekoOutline.color,
                        shape = singleBadgeShape,
                    )
                }
    ) {
        Text(
            modifier = Modifier.padding(vertical = NekoSize.tiny, horizontal = NekoSize.small),
            text = count,
            style = MaterialTheme.typography.labelMedium.copy(color = countColor),
        )
    }
}

private val singleBadgeShape =
    RoundedCornerShape(topStartPercent = 50, 25, bottomStartPercent = 25, bottomEndPercent = 50)

private val downloadBadgeShape = SlashedRoundedShape(CornerSize(50), CornerSize(25), false)

private val unreadBadgeShape = SlashedRoundedShape(CornerSize(25), CornerSize(50), true)

@Composable
private fun DoubleBadge(
    outline: Boolean,
    offset: Dp,
    backgroundColor1: Color,
    backgroundColor2: Color,
    count1: String,
    count1Color: Color,
    count2: String,
    count2Color: Color,
) {
    OverlappingLayout(
        modifier = Modifier.offset(offset, offset),
        overlap = 10.dp, // The amount of overlap
    ) {
        DoubleBadgeOneSide(
            shape = downloadBadgeShape,
            color = count1Color,
            backgroundColor = backgroundColor1,
            count = count1,
            startPadding = NekoSize.small,
            endPadding = NekoSize.small + NekoSize.extraTiny,
            outline = outline,
        )

        DoubleBadgeOneSide(
            shape = unreadBadgeShape,
            color = count2Color,
            backgroundColor = backgroundColor2,
            count = count2,
            startPadding = NekoSize.small + NekoSize.extraTiny,
            endPadding = NekoSize.small,
            outline = outline,
        )
    }
}

@Composable
private fun DoubleBadgeOneSide(
    modifier: Modifier = Modifier,
    shape: Shape,
    color: Color,
    startPadding: Dp,
    endPadding: Dp,
    backgroundColor: Color,
    count: String,
    outline: Boolean,
) {
    Box(
        modifier =
            modifier.clip(shape).background(color = backgroundColor).conditional(outline) {
                this.border(width = NekoOutline.thickness, color = NekoOutline.color, shape = shape)
            }
    ) {
        Text(
            modifier =
                Modifier.padding(
                    top = NekoSize.tiny,
                    bottom = NekoSize.tiny,
                    start = startPadding,
                    end = endPadding,
                ),
            text = count,
            style = MaterialTheme.typography.labelMedium.copy(color = color),
        )
    }
}

@Composable
fun OverlappingLayout(modifier: Modifier = Modifier, overlap: Dp, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        // This layout is designed for exactly two children
        require(measurables.size == 2) { "This layout requires exactly two children." }

        // Measure the first child
        val firstPlaceable = measurables[0].measure(constraints)

        // Measure the second child
        val secondPlaceable = measurables[1].measure(constraints)

        // Calculate the total width of the layout
        val totalWidth = (firstPlaceable.width + secondPlaceable.width) - overlap.roundToPx()

        // Calculate the total height of the layout
        val totalHeight = maxOf(firstPlaceable.height, secondPlaceable.height)

        // Place the layout on the screen
        layout(totalWidth, totalHeight) {
            // Place the first child at the start (x=0)
            firstPlaceable.placeRelative(x = 0, y = 0)

            // Place the second child with the specified overlap
            // We move it to the right by the first child's width minus the overlap
            secondPlaceable.placeRelative(x = firstPlaceable.width - overlap.roundToPx(), y = 0)
        }
    }
}

private class SlashedRoundedShape(
    private val topStart: CornerSize,
    private val bottomStart: CornerSize,
    private val isReversed: Boolean = false,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        return Outline.Generic(
            path =
                Path().apply {
                    if (!isReversed) {
                        // Original shape: rounded on left, slashed on right
                        val topStartRadius = topStart.toPx(size, density)
                        arcTo(
                            rect =
                                Rect(
                                    left = 0f,
                                    top = 0f,
                                    right = topStartRadius * 2,
                                    bottom = topStartRadius * 2,
                                ),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false,
                        )
                        lineTo(size.width, 0f)
                        lineTo(size.width - 20f, size.height)
                        val bottomStartRadius = bottomStart.toPx(size, density)
                        arcTo(
                            rect =
                                Rect(
                                    left = 0f,
                                    top = size.height - bottomStartRadius * 2,
                                    right = bottomStartRadius * 2,
                                    bottom = size.height,
                                ),
                            startAngleDegrees = 90f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false,
                        )
                        close()
                    } else {
                        // Reversed shape: slashed on left, rounded on right
                        moveTo(20f, 0f)
                        lineTo(size.width, 0f)
                        val topEndRadius = topStart.toPx(size, density)
                        arcTo(
                            rect =
                                Rect(
                                    left = size.width - topEndRadius * 2,
                                    top = 0f,
                                    right = size.width,
                                    bottom = topEndRadius * 2,
                                ),
                            startAngleDegrees = 270f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false,
                        )
                        val bottomEndRadius = bottomStart.toPx(size, density)
                        arcTo(
                            rect =
                                Rect(
                                    left = size.width - bottomEndRadius * 2,
                                    top = size.height - bottomEndRadius * 2,
                                    right = size.width,
                                    bottom = size.height,
                                ),
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false,
                        )
                        lineTo(0f, size.height)
                        close()
                    }
                }
        )
    }
}

@Preview(showBackground = true, apiLevel = 32)
@Composable
private fun DownloadUnreadBadgePreview() {
    Surface { DownloadUnreadBadge(true, true, 100, true, 100, 0.dp) }
}

@Preview(showBackground = true, apiLevel = 32)
@Composable
private fun DownloadUnreadBadgePreview10() {
    Surface { DownloadUnreadBadge(true, true, 10, true, 1, 0.dp) }
}

@Preview(showBackground = true, apiLevel = 32)
@Composable
private fun DownloadUnreadBadgePreviewSmall() {
    Surface { DownloadUnreadBadge(true, true, 1, true, 1, 0.dp) }
}

@Preview(showBackground = true, apiLevel = 32)
@Composable
private fun DownloadUnreadBadgePreview2() {
    Surface { DownloadUnreadBadge(false, true, 100, true, 100, 0.dp) }
}

@Preview(showBackground = true, apiLevel = 32)
@Composable
private fun DownloadUnreadBadgePreview3() {
    Surface { DownloadUnreadBadge(true, false, 100, true, 100, 0.dp) }
}

@Preview(showBackground = true, apiLevel = 32)
@Composable
private fun DownloadUnreadBadgePreview4() {
    Surface { DownloadUnreadBadge(false, true, 100, false, 100, 0.dp) }
}

@Preview(showBackground = true, apiLevel = 32)
@Composable
private fun DownloadUnreadBadgePreview5() {
    Surface { DownloadUnreadBadge(false, true, 1, false, 100, 0.dp) }
}
