package org.nekomanga.presentation.screens.mangadetails

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DescriptionActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.NextUnreadChapter
import eu.kanade.tachiyomi.ui.manga.MergeConstants
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.presentation.components.DynamicRippleTheme
import org.nekomanga.presentation.components.SimpleDropDownItem
import org.nekomanga.presentation.components.SimpleDropdownMenu
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun MangaDetailsHeader(
    generalState: State<MangaConstants.MangaScreenGeneralState>,
    mangaState: State<MangaConstants.MangaScreenMangaState>,
    windowSizeClass: WindowSizeClass,
    isLoggedIntoTrackersProvider: () -> Boolean,
    themeColorState: ThemeColorState,
    generatePalette: (Drawable) -> Unit = {},
    titleLongClick: (String) -> Unit = {},
    creatorLongClick: (String) -> Unit = {},
    toggleFavorite: () -> Unit = {},
    moveCategories: () -> Unit = {},
    trackingClick: () -> Unit = {},
    artworkClick: () -> Unit = {},
    similarClick: () -> Unit = {},
    mergeClick: () -> Unit = {},
    linksClick: () -> Unit = {},
    shareClick: () -> Unit = {},
    descriptionActions: DescriptionActions,
    quickReadClick: () -> Unit = {},
) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme, LocalTextSelectionColors provides themeColorState.textSelectionColors) {
        var favoriteExpanded by rememberSaveable { mutableStateOf(false) }

        val isExpanded = rememberSaveable(mangaState.value.inLibrary) {
            when (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                true -> mutableStateOf(true)
                false -> mutableStateOf(!mangaState.value.inLibrary)
            }
        }

        val backdropHeight = when (generalState.value.extraLargeBackdrop) {
            true -> (LocalConfiguration.current.screenHeightDp / 1.2).dp
            false -> (LocalConfiguration.current.screenHeightDp / 2.1).dp
        }

        Column {
            BoxWithConstraints {
                BackDrop(
                    themeColorState = themeColorState,
                    artworkProvider = { mangaState.value.currentArtwork },
                    showBackdropProvider = { generalState.value.themeBasedOffCovers },
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeightIn(250.dp, maxOf(250.dp, backdropHeight)),
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
                        titleProvider = { mangaState.value.currentTitle },
                        authorProvider = { mangaState.value.author },
                        artistProvider = { mangaState.value.artist },
                        ratingProvider = { mangaState.value.rating },
                        usersProvider = { mangaState.value.users },
                        langFlagProvider = { mangaState.value.langFlag },
                        statusProvider = { mangaState.value.status },
                        isPornographicProvider = { mangaState.value.isPornographic },
                        missingChaptersProvider = { mangaState.value.missingChapters },
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 70.dp),
                        isExpandedProvider = { isExpanded.value },
                        showMergedIconProvider = { mangaState.value.isMerged is MergeConstants.IsMergedManga.Yes && !generalState.value.hideButtonText },
                        titleLongClick = titleLongClick,
                        creatorLongClicked = creatorLongClick,
                    )
                    Gap(height = 16.dp)
                    ButtonBlock(
                        hideButtonTextProvider = { generalState.value.hideButtonText },
                        isInitializedProvider = { mangaState.value.initialized },
                        isMergedProvider = { mangaState.value.isMerged is MergeConstants.IsMergedManga.Yes },
                        inLibraryProvider = { mangaState.value.inLibrary },
                        loggedIntoTrackersProvider = isLoggedIntoTrackersProvider,
                        trackServiceCountProvider = { generalState.value.trackServiceCount },
                        themeColorState = themeColorState,
                        favoriteClick = {
                            if (!mangaState.value.inLibrary) {
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
                        themeColorState = themeColorState,
                        moveCategories = moveCategories,
                        toggleFavorite = toggleFavorite,
                        onDismiss = { favoriteExpanded = false },
                    )
                }
            }
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                QuickReadButton({ generalState.value.nextUnreadChapter }, themeColorState, quickReadClick)
            }
            Gap(8.dp)
            DescriptionBlock(
                windowSizeClass = windowSizeClass,
                titleProvider = { mangaState.value.currentTitle },
                descriptionProvider = { mangaState.value.currentDescription },
                isInitializedProvider = { mangaState.value.initialized },
                altTitlesProvider = { mangaState.value.alternativeTitles },
                genresProvider = { mangaState.value.genres },
                themeColorState = themeColorState,
                isExpanded = isExpanded.value,
                expandCollapseClick = {
                    isExpanded.value = !isExpanded.value
                },
                genreClick = descriptionActions.genreClick,
                genreLongClick = descriptionActions.genreLongClick,
                altTitleClick = descriptionActions.altTitleClick,
                altTitleResetClick = descriptionActions.altTitleResetClick,
            )
            if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
                QuickReadButton({ generalState.value.nextUnreadChapter }, themeColorState, quickReadClick)
                Gap(8.dp)
            }
        }
    }
}

@Composable
private fun ColumnScope.QuickReadButton(
    quickReadTextProvider: () -> NextUnreadChapter,
    themeColorState: ThemeColorState,
    quickReadClick: () -> Unit,
) {
    if (quickReadTextProvider().text.isNotEmpty() && quickReadTextProvider().id != null) {
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
                Text(text = stringResource(id = quickReadTextProvider().id!!, quickReadTextProvider().text), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.surface)
            }
        }
    }
}

@Composable
private fun FavoriteDropDown(favoriteExpanded: Boolean, themeColorState: ThemeColorState, moveCategories: () -> Unit, toggleFavorite: () -> Unit, onDismiss: () -> Unit) {
    SimpleDropdownMenu(
        expanded = favoriteExpanded,
        themeColorState = themeColorState,
        onDismiss = onDismiss,
        dropDownItems = persistentListOf(
            SimpleDropDownItem.Action(
                text = stringResource(R.string.remove_from_library),
                onClick = { toggleFavorite() },
            ),
            SimpleDropDownItem.Action(
                text = stringResource(R.string.edit_categories),
                onClick = { moveCategories() },
            ),
        ),

    )
}
