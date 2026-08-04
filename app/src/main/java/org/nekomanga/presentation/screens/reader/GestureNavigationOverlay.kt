package org.nekomanga.presentation.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation

@Composable
fun GestureNavigationOverlay(
    navigation: ViewerNavigation?,
    isLtr: Boolean,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible || navigation == null || navigation is DisabledNavigation) return

    var currentVisible by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) { currentVisible = visible }

    if (!currentVisible) return

    val context = LocalContext.current

    BoxWithConstraints(
        modifier =
            modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.changedToDown() }) {
                            currentVisible = false
                            onDismiss()
                        }
                    }
                }
            }
    ) {
        val width = maxWidth
        val height = maxHeight

        navigation.regions.forEach { regionItem ->
            val region = regionItem.invert(navigation.invertMode)
            val rect = region.rectF
            val directionalRegion = region.type.directionalRegion(isLtr)
            val color = Color(ContextCompat.getColor(context, directionalRegion.colorRes))

            val leftDp = width * rect.left
            val topDp = height * rect.top
            val rightDp = width * rect.right
            val bottomDp = height * rect.bottom

            val regionWidth = rightDp - leftDp
            val regionHeight = bottomDp - topDp

            Box(
                modifier =
                    Modifier.offset(x = leftDp, y = topDp)
                        .size(width = regionWidth, height = regionHeight)
                        .background(color)
            ) {
                val regionText = stringResource(directionalRegion.nameRes)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
