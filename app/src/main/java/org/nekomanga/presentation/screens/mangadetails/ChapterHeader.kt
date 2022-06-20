package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.ThemeColors

@Composable
fun ChapterHeader(themeColor: ThemeColors, numberOfChapters: Int, filterText: String = "", onClick: () -> Unit = {}) {
 
    CompositionLocalProvider(LocalRippleTheme provides themeColor.rippleTheme) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
        ) {
            ChapterText(
                numberOfChapters,
                modifier = Modifier
                    .align(Alignment.CenterStart),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd),

                horizontalArrangement = Arrangement.End,
            ) {

                if (filterText.isNotBlank()) {
                    Text(
                        text = filterText,
                        style =
                        MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaHighContrast)),
                        modifier = Modifier
                            .requiredWidthIn(0.dp, 200.dp)
                            .align(Alignment.CenterVertically)
                            .padding(end = 8.dp),
                        textAlign = TextAlign.End,
                    )
                }

                FilterIcon(
                    themeColor.buttonColor,
                    modifier = Modifier
                        .align(Alignment.CenterVertically),
                )

            }
        }
    }
}

@Composable
private fun ChapterText(numberOfChapters: Int, modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources

    Text(
        text = resources.getQuantityString(R.plurals.chapters_plural, numberOfChapters, numberOfChapters),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.highAlphaLowContrast),
        modifier = modifier,
    )
}

@Composable
private fun FilterIcon(buttonColor: Color, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.FilterList,
        modifier = modifier
            .size(32.dp),
        tint = buttonColor, contentDescription = null,
    )
}
