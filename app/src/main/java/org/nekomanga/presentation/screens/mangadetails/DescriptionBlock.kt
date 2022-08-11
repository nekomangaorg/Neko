package org.nekomanga.presentation.screens.mangadetails

import android.animation.TimeInterpolator
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.text.isDigitsOnly
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.mikepenz.markdown.Markdown
import com.mikepenz.markdown.MarkdownColors
import com.mikepenz.markdown.MarkdownDefaults
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import jp.wasabeef.gap.Gap
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.nekomanga.presentation.Chip
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun DescriptionBlock(
    manga: Manga,
    title: String,
    themeColorState: ThemeColorState,
    isExpanded: Boolean,
    isTablet: Boolean,
    canExpandCollapse: Boolean,
    expandCollapseClick: () -> Unit = {},
    genreClick: (String) -> Unit = {},
    genreLongClick: (String) -> Unit = {},
    altTitleClick: (String) -> Unit = {},
    altTitleResetClick: () -> Unit = {},
) {

    val surfaceColor = MaterialTheme.colorScheme.surface
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val isDarkTheme = isSystemInDarkTheme()
    val tagColor = remember {
        generateTagColor(surfaceColor, secondaryColor, themeColorState.buttonColor, isDarkTheme)
    }

    val noDescription = stringResource(R.string.no_description)
    var description by remember { mutableStateOf(noDescription) }

    if (MdUtil.getMangaId(manga.url).isDigitsOnly()) {
        LaunchedEffect(key1 = 1) {
            description = "THIS MANGA IS NOT MIGRATED TO V5"
        }
    } else if (manga.description != null) {
        LaunchedEffect(key1 = 1) {
            description = manga.description!!
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    val clickable = Modifier.conditional(canExpandCollapse) {
        this.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = expandCollapseClick,
        )
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .then(clickable),
        // .animateContentSize(tween(400, easing = AnticipateOvershootInterpolator().toEasing())),
    ) {

        if (!isExpanded) {
            val text = description.replace(
                Regex(
                    "[\\r\\n\\s*]{2,}",
                    setOf(RegexOption.MULTILINE),
                ),
                "\n",
            )

            val lineHeight = with(LocalDensity.current) {
                MaterialTheme.typography.bodyLarge.fontSize.toDp() + 5.dp
            }

            Box {
                Text(
                    modifier = Modifier.align(Alignment.TopStart),
                    text = text,
                    maxLines = 3,
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)),
                )
                Box(
                    modifier = Modifier
                        .height(lineHeight)
                        .align(Alignment.BottomEnd)
                        .width(150.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = .8f), MaterialTheme.colorScheme.surface),
                            ),
                        ),
                ) {
                    MoreLessButton(themeColorState.buttonColor, true, Modifier.align(Alignment.TopEnd))
                }
            }
        } else {
            if (isTablet) {
                AltTitles(
                    altTitles = manga.getAltTitles(),
                    currentTitle = title,
                    themeColorState = themeColorState,
                    tagColor = tagColor,
                    altTitleClick = altTitleClick,
                    resetClick = altTitleResetClick,
                )
                Gap(8.dp)
                Genres(manga.getGenres(), tagColor, genreClick, genreLongClick)
                Gap(16.dp)
            }
            val text = description.trim()
            SelectionContainer {
                Markdown(
                    content = text,
                    colors = markdownColors(),
                    typography = markdownTypography(),
                    flavour = CommonMarkFlavourDescriptor(),
                    modifier = clickable,
                )
            }

            if (!isTablet) {
                Gap(8.dp)
                AltTitles(
                    altTitles = manga.getAltTitles(),
                    currentTitle = title,
                    themeColorState = themeColorState,
                    tagColor = tagColor,
                    altTitleClick = altTitleClick,
                    resetClick = altTitleResetClick,
                )
                Gap(16.dp)
                Genres(manga.getGenres(), tagColor, genreClick, genreLongClick)
                Gap(16.dp)
                MoreLessButton(
                    buttonColor = themeColorState.buttonColor,
                    isMore = false,
                    clickable
                        .align(Alignment.End),
                )
            }
        }
    }
}

/**
 * Composable for the expand more and expand less button
 */
@Composable
private fun MoreLessButton(buttonColor: Color, isMore: Boolean, modifier: Modifier = Modifier) {

    val (text, icon) = when (isMore) {
        true -> R.string.more to Icons.Filled.ExpandMore
        false -> R.string.less to Icons.Filled.ExpandLess
    }

    Row(
        modifier = modifier,
    ) {
        Text(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 8.dp),
            text = stringResource(text),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = buttonColor,
            ),
        )
        Gap(4.dp)
        Icon(modifier = Modifier.align(Alignment.CenterVertically), imageVector = icon, contentDescription = null, tint = buttonColor)
    }
}

@Composable
private fun ColumnScope.AltTitles(altTitles: List<String>, currentTitle: String, tagColor: Color, themeColorState: ThemeColorState, altTitleClick: (String) -> Unit, resetClick: () -> Unit) {
    if (altTitles.isNotEmpty()) {
        val isCustomTitle = altTitles.contains(currentTitle)
        val onChipColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NekoColors.mediumAlphaHighContrast)


        Text(
            text = "Alt Titles:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        constraints.copy(
                            maxWidth = constraints.maxWidth + 32.dp.roundToPx(), //add the end padding 16.dp
                        ),
                    )
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item { Gap(8.dp) }

            if (isCustomTitle) {
                item {
                    TextButton(onClick = resetClick) {
                        Text(text = stringResource(id = R.string.reset), style = MaterialTheme.typography.labelMedium, color = themeColorState.buttonColor)
                    }
                }
            }

            items(altTitles) { title ->
                val currentlySelected = isCustomTitle && title == currentTitle
                AssistChip(
                    onClick = {
                        if (!currentlySelected) {
                            altTitleClick(title)
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(containerColor = tagColor, labelColor = onChipColor),
                    border = null,
                    leadingIcon = {
                        if (currentlySelected) {
                            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = onChipColor)
                        }
                    },
                    label = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .padding(0.dp),
                        )
                    },
                )
            }
            item { Gap(8.dp) }
        }
    }
}

@Composable
private fun ColumnScope.Genres(genres: List<String>?, tagColor: Color, genreClick: (String) -> Unit, genreLongClick: (String) -> Unit) {
    genres ?: return

    val haptic = LocalHapticFeedback.current
    Text(
        text = "Tags:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
    )
    Gap(8.dp)
    FlowRow(mainAxisAlignment = FlowMainAxisAlignment.Start, mainAxisSpacing = 12.dp, crossAxisSpacing = 12.dp) {
        genres.forEach { genre ->
            Chip(
                label = genre, containerColor = tagColor,
                modifier = Modifier.combinedClickable(
                    onClick = { genreClick(genre) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        genreLongClick(genre)
                    },
                ),
            )
        }

    }
}

@Composable
private fun markdownColors(): MarkdownColors {
    return MarkdownDefaults.markdownColors(
        textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
        backgroundColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
private fun markdownTypography() =
    MarkdownDefaults.markdownTypography(
        h1 = MaterialTheme.typography.headlineMedium,
        h2 = MaterialTheme.typography.headlineSmall,
        h3 = MaterialTheme.typography.titleLarge,
        h4 = MaterialTheme.typography.titleMedium,
        h5 = MaterialTheme.typography.titleSmall,
        h6 = MaterialTheme.typography.bodyLarge,
        body1 = MaterialTheme.typography.bodyLarge,
        body2 = MaterialTheme.typography.bodySmall,
    )

/**
 * Generates the container color for the tag, changes depending on if if the button color is the secondary color or is based off the cover
 */
private fun generateTagColor(surfaceColor: Color, secondaryColor: Color, buttonColor: Color, isDarkTheme: Boolean): Color {
    val buttonColorArray = FloatArray(3)
    val bgArray = FloatArray(3)

    ColorUtils.colorToHSL(surfaceColor.toArgb(), bgArray)
    ColorUtils.colorToHSL(buttonColor.toArgb(), buttonColorArray)

    return Color(
        ColorUtils.setAlphaComponent(
            ColorUtils.HSLToColor(
                floatArrayOf(
                    when (buttonColor == secondaryColor) {
                        true -> bgArray[0]
                        false -> buttonColorArray[0]
                    },
                    bgArray[1],
                    (
                        when {
                            isDarkTheme -> 0.225f
                            else -> 0.85f
                        }
                        ),
                ),
            ),
            199,
        ),
    )
}

fun TimeInterpolator.toEasing() = Easing { x ->
    getInterpolation(x)
}
