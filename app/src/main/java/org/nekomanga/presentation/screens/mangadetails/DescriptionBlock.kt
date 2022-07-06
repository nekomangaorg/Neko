package org.nekomanga.presentation.screens.mangadetails

import android.animation.TimeInterpolator
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun DescriptionBlock(
    manga: Manga,
    themeColor: ThemeColorState,
    isExpanded: Boolean,
    isTablet: Boolean,
    expandCollapseClick: () -> Unit = {},
    genreClick: (String) -> Unit = {},
    genreLongClick: (String) -> Unit = {},
) {

    val surfaceColor = MaterialTheme.colorScheme.surface
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val isDarkTheme = isSystemInDarkTheme()
    val tagColor = remember {
        generateTagColor(surfaceColor, secondaryColor, themeColor.buttonColor, isDarkTheme)
    }

    val description = when (MdUtil.getMangaId(manga.url).isDigitsOnly()) {
        true -> "THIS MANGA IS NOT MIGRATED TO V5"
        false -> manga.description ?: stringResource(R.string.no_description)
    }

    val interactionSource = remember { MutableInteractionSource() }
    CompositionLocalProvider(LocalRippleTheme provides themeColor.rippleTheme) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = expandCollapseClick,
                ),
            // .animateContentSize(tween(400, easing = AnticipateOvershootInterpolator().toEasing())),
        ) {

            if (isExpanded.not()) {
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
                        MoreLessButton(themeColor.buttonColor, true, Modifier.align(Alignment.TopEnd))
                    }
                }
            } else {
                val text = description.trim()
                SelectionContainer {
                    Markdown(
                        content = text,
                        colors = markdownColors(),
                        typography = markdownTypography(),
                        flavour = CommonMarkFlavourDescriptor(),
                        modifier = Modifier.clickable {
                            expandCollapseClick()
                        },
                    )
                }

                Gap(16.dp)



                Genres(manga.getGenres(), tagColor, genreClick, genreLongClick)

                if (isTablet.not()) {
                    Gap(16.dp)
                    MoreLessButton(
                        buttonColor = themeColor.buttonColor,
                        isMore = false,
                        Modifier
                            .clickable(onClick = expandCollapseClick)
                            .align(Alignment.End),
                    )
                }
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
fun Genres(genres: List<String>?, tagColor: Color, genreClick: (String) -> Unit, genreLongClick: (String) -> Unit) {
    genres ?: return

    val haptic = LocalHapticFeedback.current

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
