package org.nekomanga.presentation.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

@Composable
internal fun BasePreferenceWidget(
    modifier: Modifier = Modifier,
    title: String? = null,
    subcomponent: @Composable (ColumnScope.() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    widget: @Composable (() -> Unit)? = null,
) {
    val highlighted = LocalPreferenceHighlighted.current
    val minHeight = LocalPreferenceMinHeight.current
    Row(
        modifier =
            modifier
                .highlightBackground(highlighted)
                .sizeIn(minHeight = minHeight)
                .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
                .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.padding(start = PrefsHorizontalPadding, end = 8.dp),
                content = { icon() },
            )
        }
        Column(modifier = Modifier.weight(1f).padding(vertical = PrefsVerticalPadding)) {
            if (!title.isNullOrBlank()) {
                Text(
                    modifier = Modifier.padding(horizontal = PrefsHorizontalPadding),
                    text = title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    style =
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                )
            }
            subcomponent?.invoke(this)
        }
        if (widget != null) {
            Box(modifier = Modifier.padding(end = PrefsHorizontalPadding), content = { widget() })
        }
    }
}

internal fun Modifier.highlightBackground(highlighted: Boolean): Modifier = composed {
    var highlightFlag by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (highlighted) {
            highlightFlag = true
            delay(3.seconds)
            highlightFlag = false
        }
    }
    val highlight by
        animateColorAsState(
            targetValue =
                if (highlightFlag) {
                    MaterialTheme.colorScheme.surfaceTint.copy(alpha = .12f)
                } else {
                    Color.Transparent
                },
            animationSpec =
                if (highlightFlag) {
                    repeatable(
                        iterations = 5,
                        animation = tween(durationMillis = 200),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset =
                            StartOffset(offsetMillis = 600, offsetType = StartOffsetType.Delay),
                    )
                } else {
                    tween(200)
                },
            label = "highlight",
        )
    Modifier.background(color = highlight)
}

internal val TrailingWidgetBuffer = 16.dp
internal val PrefsHorizontalPadding = 16.dp
internal val PrefsVerticalPadding = 16.dp
