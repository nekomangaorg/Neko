package org.nekomanga.presentation.screens.mangadetails

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DescriptionActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.NextUnreadChapter
import jp.wasabeef.gap.Gap
import me.saket.cascade.CascadeDropdownMenu
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.components.DynamicRippleTheme
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun MangaDetailsHeader(
    title: String,
    author: String,
    artist: String,
    rating: String?,
    users: String?,
    langFlag: String?,
    status: Int,
    isPornographic: Boolean,
    missingChapters: String?,
    description: String,
    altTitles: List<String>,
    genres: List<String>,
    hideButtonText: Boolean,
    artwork: Artwork,
    showBackdrop: Boolean = true,
    isMerged: Boolean = true,
    inLibrary: Boolean = true,
    isTablet: Boolean = false,
    themeColorState: ThemeColorState,
    generatePalette: (Drawable) -> Unit = {},
    titleLongClick: (String) -> Unit = {},
    creatorLongClick: (String) -> Unit = {},
    loggedIntoTrackers: Boolean,
    trackServiceCount: Int,
    toggleFavorite: () -> Unit = {},
    moveCategories: () -> Unit = {},
    trackingClick: () -> Unit = {},
    artworkClick: () -> Unit = {},
    similarClick: () -> Unit = {},
    mergeClick: () -> Unit = {},
    linksClick: () -> Unit = {},
    shareClick: () -> Unit = {},
    descriptionActions: DescriptionActions,
    quickReadText: NextUnreadChapter,
    quickReadClick: () -> Unit = {},
) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme, LocalTextSelectionColors provides themeColorState.textSelectionColors) {

        var favoriteExpanded by rememberSaveable { mutableStateOf(false) }

        val isExpanded = rememberSaveable {
            when (isTablet) {
                false -> mutableStateOf(!inLibrary)
                true -> mutableStateOf(true)
            }
        }

        Column {
            Box {
                BackDrop(
                    themeColorState = themeColorState,
                    artworkProvider = { artwork },
                    showBackdrop = showBackdrop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeightIn(250.dp, 400.dp),
                    generatePalette = generatePalette,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface),
                            ),
                        ),
                )

                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    InformationBlock(
                        title = title,
                        author = author,
                        artist = artist,
                        rating = rating,
                        users = users,
                        langFlag = langFlag,
                        status = status,
                        isPornographic = isPornographic,
                        missingChapters = missingChapters,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 70.dp),
                        isExpanded = isExpanded.value,
                        showMergedIcon = isMerged && !hideButtonText,
                        titleLongClick = titleLongClick,
                        creatorLongClicked = creatorLongClick,
                    )
                    Gap(height = 16.dp)
                    ButtonBlock(
                        hideButtonText = hideButtonText,
                        isMerged = isMerged,
                        inLibrary = inLibrary,
                        loggedIntoTrackers = loggedIntoTrackers,
                        trackServiceCount = trackServiceCount,
                        themeColorState = themeColorState,
                        favoriteClick = {
                            if (!inLibrary) {
                                toggleFavorite()
                            } else {
                                favoriteExpanded = true
                            }
                        },

                        trackingClick = trackingClick,
                        artworkClick = artworkClick,
                        similarClick = similarClick,
                        mergeClick = mergeClick,
                        linksClick = linksClick,
                        shareClick = shareClick,
                    )
                    FavoriteDropDown(
                        favoriteExpanded = favoriteExpanded,
                        moveCategories = moveCategories,
                        toggleFavorite = toggleFavorite,
                        onDismiss = { favoriteExpanded = false },
                    )
                }
            }
            if (isTablet && quickReadText.text.isNotEmpty() && quickReadText.id != null) {
                QuickReadButton(quickReadText, themeColorState, quickReadClick)
            }
            Gap(8.dp)
            DescriptionBlock(
                title = title,
                description = description,
                altTitles = altTitles,
                genres = genres,
                themeColorState = themeColorState,
                isExpanded = isExpanded.value,
                isTablet = isTablet,
                canExpandCollapse = !isTablet,
                expandCollapseClick = {
                    isExpanded.value = !isExpanded.value
                },
                genreClick = descriptionActions.genreClick,
                genreLongClick = descriptionActions.genreLongClick,
                altTitleClick = descriptionActions.altTitleClick,
                altTitleResetClick = descriptionActions.altTitleResetClick,
            )
            if (!isTablet && quickReadText.text.isNotEmpty() && quickReadText.id != null) {
                QuickReadButton(quickReadText, themeColorState, quickReadClick)
                Gap(8.dp)
            }
        }
    }
}

@Composable
private fun ColumnScope.QuickReadButton(
    quickReadText: NextUnreadChapter,
    themeColorState: ThemeColorState,
    quickReadClick: () -> Unit,
) {
    if (quickReadText.text.isNotEmpty() && quickReadText.id != null) {
        Gap(8.dp)
        CompositionLocalProvider(LocalRippleTheme provides DynamicRippleTheme(themeColorState.altContainerColor)) {
            ElevatedButton(
                onClick = quickReadClick,
                shape = RoundedCornerShape(35),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = ButtonDefaults.elevatedButtonColors(containerColor = themeColorState.buttonColor),
            ) {
                Text(text = stringResource(id = quickReadText.id, quickReadText.text), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.surface)
            }
        }
    }
}

@Composable
private fun FavoriteDropDown(favoriteExpanded: Boolean, moveCategories: () -> Unit, toggleFavorite: () -> Unit, onDismiss: () -> Unit) {
    CascadeDropdownMenu(
        expanded = favoriteExpanded,
        offset = DpOffset(8.dp, 0.dp),
        onDismissRequest = onDismiss,
    ) {
        val style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, letterSpacing = (-.5).sp)
        androidx.compose.material3.DropdownMenuItem(
            text = {
                androidx.compose.material.Text(
                    text = stringResource(R.string.remove_from_library),
                    style = style,
                )
            },
            onClick = {
                toggleFavorite()
                onDismiss()
            },
        )
        androidx.compose.material3.DropdownMenuItem(
            text = {
                androidx.compose.material.Text(
                    text = stringResource(R.string.edit_categories),
                    style = style,
                )
            },
            onClick = {
                moveCategories()
                onDismiss()
            },
        )
    }
}

