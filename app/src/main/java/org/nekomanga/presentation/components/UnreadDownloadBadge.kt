package org.nekomanga.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.theme.Size

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
                .background(backgroundColor, badgeShape)
                .conditional(outline) {
                    this.border(
                        width = Outline.thickness,
                        color = Outline.color,
                        shape = badgeShape,
                    )
                }
    ) {
        Text(
            modifier = Modifier.padding(vertical = Size.tiny, horizontal = Size.tiny),
            text = count,
            style = MaterialTheme.typography.labelMedium.copy(color = countColor),
        )
    }
}

private val badgeShape =
    RoundedCornerShape(topStartPercent = 50, 25, bottomStartPercent = 25, bottomEndPercent = 50)

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
    val gradientBrush =
        Brush.linearGradient(
            colorStops = listOf(0.5f to backgroundColor1, 0.5f to backgroundColor2).toTypedArray()
        )

    Box(
        modifier =
            Modifier.offset(x = offset, y = offset)
                .clip(badgeShape)
                .background(brush = gradientBrush)
                .conditional(outline) {
                    this.border(
                        width = Outline.thickness,
                        color = Outline.color,
                        shape = badgeShape,
                    )
                }
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = Size.extraTiny, horizontal = Size.tiny),
        ) {
            Text(
                text = count1.toString(),
                style = MaterialTheme.typography.labelMedium.copy(color = count1Color),
            )
            Gap(Size.small)

            Text(
                text = count2,
                style = MaterialTheme.typography.labelMedium.copy(color = count2Color),
            )
        }
    }
}

@Preview(showBackground = true, apiLevel = 32)
@Composable
private fun DownloadUnreadBadgePreview() {
    Surface { DownloadUnreadBadge(true, true, 100, true, 100, 0.dp) }
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
