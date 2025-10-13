package org.nekomanga.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
    minFontSize: TextUnit = 12.sp,
) {
    var scaledTextStyle by remember(text, style) { mutableStateOf(style) }
    var isTextReady by remember(text, style) { mutableStateOf(false) }

    Layout(
        modifier = modifier,
        content = {
            Text(
                text = text,
                style = scaledTextStyle,
                softWrap = false,
                maxLines = maxLines,
                textAlign = textAlign,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    if (isTextReady) {
                        return@Text
                    }

                    if (textLayoutResult.didOverflowWidth || textLayoutResult.didOverflowHeight) {
                        val newFontSize = scaledTextStyle.fontSize * 0.95
                        if (newFontSize <= minFontSize) {
                            scaledTextStyle = scaledTextStyle.copy(fontSize = minFontSize)
                            isTextReady = true
                        } else {
                            scaledTextStyle = scaledTextStyle.copy(fontSize = newFontSize)
                        }
                    } else {
                        isTextReady = true
                    }
                },
            )
        },
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)

        if (isTextReady) {
            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        } else {
            layout(0, 0) {}
        }
    }
}
