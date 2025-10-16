package org.nekomanga.presentation.screens.mangadetails

import android.animation.TimeInterpolator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.MarkdownState
import com.mikepenz.markdown.model.rememberMarkdownState
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.nekomanga.R
import org.nekomanga.presentation.Chip
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dropdown.SimpleDropDownItem
import org.nekomanga.presentation.components.dropdown.SimpleDropdownMenu
import org.nekomanga.presentation.extensions.surfaceColorAtElevationCustomColor
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun DescriptionBlock(
    windowSizeClass: WindowSizeClass,
    title: String,
    description: String,
    isInitialized: Boolean,
    altTitles: PersistentList<String>,
    genres: PersistentList<String>,
    themeColorState: ThemeColorState,
    wrapAltTitles: Boolean,
    isExpanded: Boolean,
    expandCollapseClick: () -> Unit,
    genreSearch: (String) -> Unit,
    genreSearchLibrary: (String) -> Unit,
    altTitleClick: (String) -> Unit,
    altTitleResetClick: () -> Unit,
) {
    if (!isInitialized) return

    val isTablet = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    val smallDescription =
        remember(description) { description.split("\n").take(2).joinToString("\n") }

    val collapsedMarkdownState =
        rememberMarkdownState(
            content = smallDescription,
            flavour = CommonMarkFlavourDescriptor(),
            immediate = true,
        )
    val expandedMarkdownState =
        rememberMarkdownState(
            content = description,
            flavour = CommonMarkFlavourDescriptor(),
            immediate = true,
        )

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = Size.medium)
                .clickable(
                    enabled = !isTablet,
                    onClick = expandCollapseClick,
                    indication = null, // No ripple effect
                    interactionSource = remember { MutableInteractionSource() },
                )
    ) {
        // Display Alt Titles and Genres first on tablet for a better layout
        if (isExpanded && isTablet) {
            AltTitlesAndGenres(
                altTitles = altTitles,
                genres = genres,
                currentTitle = title,
                shouldWrap = wrapAltTitles,
                themeColorState = themeColorState,
                altTitleClick = altTitleClick,
                resetClick = altTitleResetClick,
                genreSearch = genreSearch,
                genreLibrarySearch = genreSearchLibrary,
            )
            Gap(Size.medium)
        }

        // Animated content for the description to smoothly transition between collapsed/expanded
        // states
        AnimatedContent(
            targetState = isExpanded,
            label = "descriptionAnimation",
            transitionSpec = {
                // The animation for the new content entering the screen
                val enter = fadeIn(animationSpec = tween(300))

                // The animation for the old content leaving the screen
                val exit = fadeOut(animationSpec = tween(300))

                // Combine the enter and exit transitions and animate the size change
                (enter togetherWith exit).using(SizeTransform(clip = true))
            },
        ) { expanded ->
            if (expanded) {
                SelectionContainer {
                    Markdown(
                        markdownState = expandedMarkdownState,
                        colors = nekoMarkdownColors(),
                        typography = nekoMarkdownTypography(),
                    )
                }
            } else {
                CollapsedDescription(markdownState = collapsedMarkdownState) {
                    MoreLessButton(
                        buttonColor = themeColorState.primaryColor,
                        isMore = true,
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }
            }
        }

        // Display Alt Titles and Genres below the description on mobile
        if (isExpanded && !isTablet) {
            Gap(Size.tiny)
            AltTitlesAndGenres(
                altTitles = altTitles,
                genres = genres,
                currentTitle = title,
                shouldWrap = wrapAltTitles,
                themeColorState = themeColorState,
                altTitleClick = altTitleClick,
                resetClick = altTitleResetClick,
                genreSearch = genreSearch,
                genreLibrarySearch = genreSearchLibrary,
            )
            Gap(Size.medium)
            MoreLessButton(
                buttonColor = themeColorState.primaryColor,
                isMore = false,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

/** A container for the collapsed description that shows a "More..." button with a gradient fade. */
@Composable
private fun CollapsedDescription(
    markdownState: MarkdownState,
    moreButton: @Composable BoxScope.() -> Unit,
) {

    val lineHeight =
        with(LocalDensity.current) {
            MaterialTheme.typography.bodyLarge.fontSize.toDp() + Size.small
        }

    val descriptionHeight =
        with(LocalDensity.current) { MaterialTheme.typography.bodyLarge.fontSize.toDp() * 3 }

    Box(modifier = Modifier.clipToBounds()) {
        Markdown(
            markdownState = markdownState,
            colors = nekoMarkdownColors(),
            typography = nekoMarkdownTypography(),
            modifier = Modifier.fillMaxWidth().heightIn(Size.none, descriptionHeight),
        )
        // Gradient overlay to fade out the text
        Box(
            modifier =
                Modifier.height(lineHeight)
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth(.40f)
                    .background(
                        Brush.horizontalGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = .8f),
                                    MaterialTheme.colorScheme.surface,
                                )
                        )
                    )
        )
        moreButton()
    }
}

/** A combined, reusable composable for displaying both Alt Titles and Genres. */
@Composable
private fun AltTitlesAndGenres(
    altTitles: PersistentList<String>,
    genres: PersistentList<String>,
    currentTitle: String,
    shouldWrap: Boolean,
    themeColorState: ThemeColorState,
    altTitleClick: (String) -> Unit,
    resetClick: () -> Unit,
    genreSearch: (String) -> Unit,
    genreLibrarySearch: (String) -> Unit,
) {
    val tagColor =
        MaterialTheme.colorScheme.surfaceColorAtElevationCustomColor(
            themeColorState.primaryColor,
            Size.medium,
        )

    AltTitles(
        altTitles = altTitles,
        currentTitle = currentTitle,
        shouldWrap = shouldWrap,
        tagColor = tagColor,
        themeColorState = themeColorState,
        altTitleClick = altTitleClick,
        resetClick = resetClick,
    )

    Spacer(modifier = Modifier.size(Size.medium))

    Genres(
        genres = genres,
        tagColor = tagColor,
        themeColorState = themeColorState,
        genreSearch = genreSearch,
        genreLibrarySearch = genreLibrarySearch,
    )
}

/**
 * A unified composable for displaying Alternative Titles, switching between FlowRow and LazyRow.
 */
@Composable
private fun AltTitles(
    altTitles: PersistentList<String>,
    currentTitle: String,
    shouldWrap: Boolean,
    tagColor: Color,
    themeColorState: ThemeColorState,
    altTitleClick: (String) -> Unit,
    resetClick: () -> Unit,
) {
    if (altTitles.isEmpty()) return

    val isCustomTitle = altTitles.contains(currentTitle)

    Column {
        Text(
            text = "Alt Titles:",
            style = MaterialTheme.typography.labelLarge,
            color =
                MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
        )

        val content: @Composable () -> Unit = {
            if (isCustomTitle) {
                TextButton(onClick = resetClick) {
                    Text(
                        text = stringResource(id = R.string.reset),
                        style = MaterialTheme.typography.labelLarge,
                        color = themeColorState.primaryColor,
                    )
                }
            }
            altTitles.forEach { title ->
                AltTitleChip(
                    title = title,
                    isSelected = isCustomTitle && title == currentTitle,
                    tagColor = tagColor,
                    onClick = { altTitleClick(title) },
                )
            }
        }

        if (shouldWrap) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Size.small),
                content = { content() },
            )
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Size.tiny),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // LazyRow requires items to be emitted this way
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Size.tiny),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

/** A single, reusable AssistChip for an alternative title. */
@Composable
private fun AltTitleChip(title: String, isSelected: Boolean, tagColor: Color, onClick: () -> Unit) {
    AssistChip(
        onClick = { if (!isSelected) onClick() },
        colors =
            AssistChipDefaults.assistChipColors(
                containerColor = tagColor,
                labelColor =
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = NekoColors.mediumAlphaHighContrast
                    ),
            ),
        border = null,
        leadingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = NekoColors.mediumAlphaHighContrast
                        ),
                )
            }
        },
        label = { Text(text = title, style = MaterialTheme.typography.labelLarge) },
    )
}

/**
 * Displays genre tags with a shared dropdown menu for search actions. State management is
 * simplified to track the selected genre directly.
 */
@Composable
private fun Genres(
    genres: PersistentList<String>,
    tagColor: Color,
    themeColorState: ThemeColorState,
    genreSearch: (String) -> Unit,
    genreLibrarySearch: (String) -> Unit,
) {
    if (genres.isEmpty()) return

    var selectedGenre by remember { mutableStateOf<String?>(null) }

    Column {
        Text(
            text = "Tags:",
            style = MaterialTheme.typography.labelLarge,
            color =
                MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
        )
        Gap(Size.tiny)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Size.smedium, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(Size.smedium),
        ) {
            genres.forEach { genre ->
                Chip(
                    label = genre,
                    containerColor = tagColor,
                    modifier = Modifier.clickable { selectedGenre = genre },
                )
            }
        }

        selectedGenre?.let { genre ->
            SimpleDropdownMenu(
                expanded = true,
                onDismiss = { selectedGenre = null },
                themeColorState = themeColorState,
                dropDownItems =
                    persistentListOf(
                        SimpleDropDownItem.Action(text = UiText.StringResource(R.string.search)) {
                            genreSearch(genre)
                            selectedGenre = null
                        },
                        SimpleDropDownItem.Action(
                            text = UiText.StringResource(R.string.search_library)
                        ) {
                            genreLibrarySearch(genre)
                            selectedGenre = null
                        },
                    ),
            )
        }
    }
}

/** Composable for the expand more and expand less button */
@Composable
private fun MoreLessButton(buttonColor: Color, isMore: Boolean, modifier: Modifier = Modifier) {
    val (text, icon) =
        when (isMore) {
            true -> R.string.more to Icons.Filled.ExpandMore
            false -> R.string.less to Icons.Filled.ExpandLess
        }

    Row(modifier = modifier) {
        Text(
            modifier =
                Modifier.background(MaterialTheme.colorScheme.surface).padding(start = Size.small),
            text = stringResource(text),
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = buttonColor,
                ),
        )
        Gap(Size.tiny)
        Icon(
            modifier = Modifier.align(Alignment.CenterVertically),
            imageVector = icon,
            contentDescription = null,
            tint = buttonColor,
        )
    }
}

@Composable
private fun nekoMarkdownColors() =
    markdownColor(
        text = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
        codeBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
    )

@Composable
private fun nekoMarkdownTypography() =
    markdownTypography(
        h1 = MaterialTheme.typography.headlineMedium,
        h2 = MaterialTheme.typography.headlineSmall,
        h3 = MaterialTheme.typography.titleLarge,
        h4 = MaterialTheme.typography.titleMedium,
        h5 = MaterialTheme.typography.titleSmall,
        h6 = MaterialTheme.typography.bodyLarge,
        paragraph = MaterialTheme.typography.bodyLarge,
        text = MaterialTheme.typography.bodyLarge,
        ordered = MaterialTheme.typography.bodyLarge,
        bullet = MaterialTheme.typography.bodyLarge,
        quote = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
        list = MaterialTheme.typography.bodyLarge,
    )

fun TimeInterpolator.toEasing() = Easing { x -> getInterpolation(x) }
