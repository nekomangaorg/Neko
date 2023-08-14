package org.nekomanga.presentation.screens.mangadetails

import android.animation.TimeInterpolator
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.mikepenz.markdown.Markdown
import com.mikepenz.markdown.MarkdownColors
import com.mikepenz.markdown.MarkdownDefaults
import com.skydoves.balloon.compose.Balloon
import com.skydoves.balloon.compose.BalloonWindow
import eu.kanade.tachiyomi.R
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.nekomanga.presentation.Chip
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.extensions.surfaceColorAtElevationCustomColor
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Padding
import toolTipBuilder

/**
 * Genre, alt titles, description
 */
@Composable
fun DescriptionBlock(
    windowSizeClass: WindowSizeClass,
    titleProvider: () -> String,
    descriptionProvider: () -> String,
    isInitializedProvider: () -> Boolean,
    altTitlesProvider: () -> ImmutableList<String>,
    genresProvider: () -> ImmutableList<String>,
    themeColorState: ThemeColorState,
    wrapAltTitles: Boolean,
    isExpanded: Boolean,
    expandCollapseClick: () -> Unit = {},
    genreSearch: (String) -> Unit = {},
    genreSearchLibrary: (String) -> Unit = {},
    altTitleClick: (String) -> Unit = {},
    altTitleResetClick: () -> Unit = {},
) {
    if (!isInitializedProvider()) return

    val tagColor = MaterialTheme.colorScheme.surfaceColorAtElevationCustomColor(themeColorState.buttonColor, 16.dp)

    val interactionSource = remember { MutableInteractionSource() }

    val clickable = Modifier.conditional(windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
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
            val text = descriptionProvider().split("\n").take(2).joinToString("\n")

            val lineHeight = with(LocalDensity.current) {
                MaterialTheme.typography.bodyLarge.fontSize.toDp() + 8.dp
            }

            val descriptionHeight = with(LocalDensity.current) {
                MaterialTheme.typography.bodyLarge.fontSize.toDp() * 3
            }

            Box {

                Markdown(
                    content = text,
                    colors = nekoMarkdownColors(),
                    typography = nekoMarkdownTypography(),
                    flavour = CommonMarkFlavourDescriptor(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .heightIn(0.dp, descriptionHeight)
                        .then(clickable),
                )

                Box(
                    modifier = Modifier
                        .height(lineHeight)
                        .align(Alignment.BottomEnd)
                        .width(175.dp)
                        .then(clickable)
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
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                AltTitles(
                    altTitles = altTitlesProvider(),
                    currentTitle = titleProvider(),
                    tagColor = tagColor,
                    themeColorState = themeColorState,
                    altTitleClick = altTitleClick,
                    resetClick = altTitleResetClick,
                    shouldWrap = wrapAltTitles,
                )
                Gap(8.dp)
                Genres(genresProvider(), tagColor, themeColorState.buttonColor, genreSearch, genreSearchLibrary)
                Gap(16.dp)
            }
            val text = descriptionProvider().trim()

            SelectionContainer {
                Markdown(
                    content = text,
                    colors = nekoMarkdownColors(),
                    typography = nekoMarkdownTypography(),
                    flavour = CommonMarkFlavourDescriptor(),
                    modifier = clickable,
                )
            }

            if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
                Gap(8.dp)
                AltTitles(
                    altTitles = altTitlesProvider(),
                    currentTitle = titleProvider(),
                    shouldWrap = wrapAltTitles,
                    tagColor = tagColor,
                    themeColorState = themeColorState,
                    altTitleClick = altTitleClick,
                    resetClick = altTitleResetClick,
                )
                Gap(16.dp)
                Genres(genresProvider(), tagColor, themeColorState.buttonColor, genreSearch, genreSearchLibrary)
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
private fun AltTitles(
    altTitles: ImmutableList<String>,
    currentTitle: String,
    shouldWrap: Boolean,
    tagColor: Color,
    themeColorState: ThemeColorState,
    altTitleClick: (String) -> Unit,
    resetClick: () -> Unit,
) {
    if (altTitles.isNotEmpty()) {
        val isCustomTitle = altTitles.contains(currentTitle)
        val onChipColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = NekoColors.mediumAlphaHighContrast)

        Text(
            text = "Alt Titles:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
        )
        if (shouldWrap) {
            FlowableAltTitles(
                altTitles = altTitles,
                currentTitle = currentTitle,
                isCustomTitle = isCustomTitle,
                tagColor = tagColor,
                themeColorState = themeColorState,
                altTitleClick = altTitleClick,
                resetClick = resetClick,
                onChipColor = onChipColor,
            )
        } else {
            ScrollableAltTitles(
                altTitles = altTitles,
                currentTitle = currentTitle,
                isCustomTitle = isCustomTitle,
                tagColor = tagColor,
                themeColorState = themeColorState,
                altTitleClick = altTitleClick,
                resetClick = resetClick,
                onChipColor = onChipColor,
            )
        }
    }
}

@Composable
private fun FlowableAltTitles(
    altTitles: ImmutableList<String>,
    currentTitle: String,
    isCustomTitle: Boolean,
    tagColor: Color,
    themeColorState: ThemeColorState,
    altTitleClick: (String) -> Unit,
    resetClick: () -> Unit,
    onChipColor: Color,
) {
    FlowRow(modifier = Modifier.fillMaxWidth(), mainAxisSpacing = Padding.small) {
        if (isCustomTitle) {
            TextButton(onClick = resetClick) {
                Text(text = stringResource(id = R.string.reset), style = MaterialTheme.typography.labelMedium, color = themeColorState.buttonColor)
            }
        }
        altTitles.forEach { title ->
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
    }
}

@Composable
private fun ScrollableAltTitles(
    altTitles: ImmutableList<String>,
    currentTitle: String,
    isCustomTitle: Boolean,
    tagColor: Color,
    themeColorState: ThemeColorState,
    altTitleClick: (String) -> Unit,
    resetClick: () -> Unit,
    onChipColor: Color,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = constraints.maxWidth + Padding.medium.roundToPx(),
                        maxWidth = constraints.maxWidth + Padding.medium.roundToPx(),
                    ),
                )
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            },
        horizontalArrangement = Arrangement.spacedBy(Padding.tiny),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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

@Composable
private fun ColumnScope.Genres(genres: ImmutableList<String>, tagColor: Color, buttonColor: Color, genreSearch: (String) -> Unit, genreLibrarySearch: (String) -> Unit) {
    if (genres.isEmpty()) return

    Text(
        text = "Tags:",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
    )
    Gap(8.dp)
    FlowRow(
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = constraints.maxWidth + 16.dp.roundToPx(),
                    maxWidth = constraints.maxWidth + 16.dp.roundToPx(),
                ),
            )
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        },
        mainAxisAlignment = FlowMainAxisAlignment.Start,
        mainAxisSpacing = 12.dp,
        crossAxisSpacing = 12.dp,
    ) {
        genres.forEach { genre ->
            var balloonWindow: BalloonWindow? by remember { mutableStateOf(null) }

            Balloon(
                builder = toolTipBuilder(backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevationCustomColor(tagColor, 16.dp), dismissable = false),
                balloonContent = {
                    GenreBalloon(balloonWindow, genreSearch, genre, buttonColor, genreLibrarySearch)
                },
            ) { window ->

                LaunchedEffect(Unit) {
                    balloonWindow = window
                }
                Chip(
                    label = genre,
                    containerColor = tagColor,
                    modifier = Modifier.clickable {
                        window.showAsDropDown()
                    },
                )
            }
        }
    }
}

@Composable
private fun GenreBalloon(
    balloonWindow: BalloonWindow?,
    genreSearch: (String) -> Unit,
    genre: String,
    buttonColor: Color,
    genreLibrarySearch: (String) -> Unit,
) {
    Row {
        TextButton(
            onClick = {
                balloonWindow?.dismiss()
                genreSearch(genre)
            },
        ) {
            Text(text = stringResource(id = R.string.search), color = buttonColor)
        }
        Gap(4.dp)
        TextButton(
            onClick = {
                balloonWindow?.dismiss()
                genreLibrarySearch(genre)
            },
        ) {
            Text(text = stringResource(id = R.string.search_library), color = buttonColor)
        }
    }
}

@Composable
private fun nekoMarkdownColors(): MarkdownColors = MarkdownDefaults.markdownColors(
    textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
    backgroundColor = MaterialTheme.colorScheme.surface,
)

@Composable
private fun nekoMarkdownTypography() =
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

fun TimeInterpolator.toEasing() = Easing { x ->
    getInterpolation(x)
}
