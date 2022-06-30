package org.nekomanga.presentation.components.sheets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import jp.wasabeef.gap.Gap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.nekomanga.presentation.screens.ThemeColors
import org.nekomanga.presentation.theme.Shapes
import java.lang.Math.abs

@Composable
fun ArtworkSheet(themeColors: ThemeColors, artworkLinks: List<String>) {
    BaseSheet(themeColors = themeColors, maxSheetHeightPercentage = .9f, minSheetHeightPercentage = .9f) {

        var showProgress by remember { mutableStateOf(false) }

        SubcomposeAsyncImage(
            model = artworkLinks[0],
            contentDescription = null,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(Shapes.coverRadius)),
        ) {
            val state = painter.state
            if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                showProgress = true
            } else {
                showProgress = false
                SubcomposeAsyncImageContent()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (showProgress) {
                CircularProgressIndicator(
                    color = themeColors.buttonColor,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .align(Alignment.BottomStart),
            ) {
                FilledIconButton(onClick = { /*TODO*/ }, modifier = Modifier.weight(1f)) {
                    Text(text = "Save", color = MaterialTheme.colorScheme.surface)
                }
                Gap(8.dp)
                FilledIconButton(onClick = { /*TODO*/ }, modifier = Modifier.weight(1f)) {
                    Text(text = "Set as Cover", color = MaterialTheme.colorScheme.surface)
                }
            }
        }
    }
}

private open class DragState(
    val index: Int,
    val screenWidth: Float,
    private val scope: CoroutineScope,
    private val animationSpec: AnimationSpec<Float>,
) {

    var opacity = Animatable(1f)
        private set

    var offsetX = Animatable(0f)
        private set
    var offsetY = Animatable(0f)
        private set
    var scale = Animatable(0f)
        private set

    private val dragFraction = mutableStateOf(0f)

    suspend fun drag(dragAmountX: Float, dragAmountY: Float, onParallel: suspend () -> Unit = {}) = scope.launch {
        dragFraction.value = abs(dragAmountX).div(screenWidth).coerceIn(0f, 1f)
        launch {
            offsetX.snapTo(dragAmountX)
        }
        launch {
            onParallel()
        }
    }

    fun positionToCenter(onParallel: suspend () -> Unit = {}) = scope.launch {
        launch { offsetX.animateTo(0f, animationSpec = animationSpec) }
        launch { offsetY.animateTo(0f, animationSpec = animationSpec) }
        launch { onParallel() }
    }

    fun animateOutsideOfScreen(onParallel: suspend () -> Unit = {}) = scope.launch {
        launch {
            offsetX.animateTo(-screenWidth, animationSpec = animationSpec)
        }
        launch {
            onParallel()
        }
    }

    suspend fun snap(scaleP: Float = 1f, opacityP: Float = 1f, offsetXP: Float = 0f) = scope.launch {
        launch { scale.snapTo(scaleP) }
        launch { opacity.snapTo(opacityP) }
        launch { offsetX.snapTo(offsetXP) }
    }

    suspend fun animateTo(
        scaleP: Float = 1f,
        opacityP: Float = 1f,
        offsetXP: Float = 0f,
        onParallel: suspend () -> Unit = {},
    ) = scope.launch {
        launch { scale.animateTo(scaleP, animationSpec = animationSpec) }
        launch { opacity.animateTo(opacityP, animationSpec = animationSpec) }
        launch { offsetX.animateTo(offsetXP, animationSpec = animationSpec) }
        launch { onParallel() }
    }
}
