package org.nekomanga.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import org.nekomanga.presentation.theme.Size

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRefresh(
    enabled: Boolean = true,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    trackColor: Color = MaterialTheme.colorScheme.secondary,
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()

    if (enabled) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = state,
            indicator = {
                WavyLinearIndicator(
                    state = state,
                    isRefreshing = isRefreshing,
                    color = trackColor,
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
                )
            },
        ) {
            content()
        }
    } else {
        Box { content() }
    }
}

@Composable
private fun WavyLinearIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val targetProgress = state.distanceFraction.coerceIn(0f, 1f)

    val animatedProgress by
        animateFloatAsState(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = 150),
            label = "WavyProgressAnimation",
        )

    Box(modifier = modifier.fillMaxWidth()) {
        val strokeWidth = with(LocalDensity.current) { Size.tiny.toPx() }
        val stroke = remember(strokeWidth) { Stroke(width = strokeWidth, cap = StrokeCap.Round) }
        if (isRefreshing) {
            LinearWavyProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart),
                color = color,
                trackColor = color.copy(alpha = 0.24f),
                stroke = stroke,
                trackStroke = stroke,
            )
        } else if (animatedProgress > 0f) {
            LinearWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart),
                color = color,
                trackColor = color.copy(alpha = 0.24f),
                stroke = stroke,
                trackStroke = stroke,
            )
        }
    }
}
