package org.nekomanga.presentation.screens.manga

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.theme.Size

/** Header that is shown above chapter list */
@Composable
fun ChapterHeader(
    themeColor: ThemeColorState,
    numberOfChapters: Int,
    filterText: String,
    onClick: () -> Unit = {},
) {
    if (numberOfChapters > 0 || filterText.isNotBlank()) {
        CompositionLocalProvider(LocalRippleConfiguration provides themeColor.rippleConfiguration) {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable(onClick = onClick)
                        .padding(horizontal = Size.small, vertical = Size.smedium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChapterText(numberOfChapters)
                Spacer(Modifier.weight(1f))

                if (filterText.isNotBlank()) {
                    Text(
                        text = filterText,
                        color =
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = NekoColors.mediumAlphaLowContrast
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.requiredWidthIn(max = 200.dp).padding(end = Size.small),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                FilterIcon(themeColor.primaryColor)
                Gap(Size.tiny)
            }
        }
    }
}

@Composable
private fun ChapterText(numberOfChapters: Int, modifier: Modifier = Modifier) {
    Text(
        text = pluralStringResource(R.plurals.chapters_plural, numberOfChapters, numberOfChapters),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
private fun FilterIcon(buttonColor: Color, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.FilterList,
        modifier = modifier.size(Size.large),
        tint = buttonColor,
        contentDescription = null,
    )
}
