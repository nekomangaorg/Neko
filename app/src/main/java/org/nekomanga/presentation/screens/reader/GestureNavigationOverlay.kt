package org.nekomanga.presentation.screens.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.ui.reader.domain.GetNavigationRegionsUseCase
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.RightAndLeftNavigation
import org.nekomanga.presentation.theme.NekoTheme

@Composable
fun GestureNavigationOverlay(
    regions: List<NavigationRegionUiModel>,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (regions.isEmpty()) return

    val currentOnDismiss by rememberUpdatedState(onDismiss)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(500)),
        modifier = modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(
            modifier =
                Modifier.fillMaxSize().pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        currentOnDismiss()
                    }
                }
        ) {
            val width = maxWidth
            val height = maxHeight

            regions.forEach { region ->
                key(region.id) {
                    val leftDp = width * region.leftFraction
                    val topDp = height * region.topFraction
                    val rightDp = width * region.rightFraction
                    val bottomDp = height * region.bottomFraction

                    val regionWidth = rightDp - leftDp
                    val regionHeight = bottomDp - topDp
                    val regionText = region.label.asString()

                    Box(
                        modifier =
                            Modifier.offset(x = leftDp, y = topDp)
                                .size(width = regionWidth, height = regionHeight)
                                .background(region.color)
                                .clearAndSetSemantics { contentDescription = regionText },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = regionText,
                            color = Color.Black,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            style =
                                LocalTextStyle.current.copy(
                                    drawStyle =
                                        Stroke(miter = 10f, width = 6f, join = StrokeJoin.Round)
                                ),
                        )
                        Text(
                            text = regionText,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun GestureNavigationOverlayKindlishPreview() {
    NekoTheme {
        GestureNavigationOverlay(
            regions = GetNavigationRegionsUseCase()(KindlishNavigation()),
            visible = true,
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun GestureNavigationOverlayLPreview() {
    NekoTheme {
        GestureNavigationOverlay(
            regions = GetNavigationRegionsUseCase()(LNavigation()),
            visible = true,
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun GestureNavigationOverlayRightAndLeftPreview() {
    NekoTheme {
        GestureNavigationOverlay(
            regions = GetNavigationRegionsUseCase()(RightAndLeftNavigation()),
            visible = true,
            onDismiss = {},
        )
    }
}
