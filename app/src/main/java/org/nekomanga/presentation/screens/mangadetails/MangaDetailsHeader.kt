package org.nekomanga.presentation.screens.mangadetails

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DescriptionActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.InformationActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.NextUnreadChapter
import eu.kanade.tachiyomi.ui.manga.MergeConstants
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.presentation.components.DynamicRippleTheme
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dropdown.SimpleDropDownItem
import org.nekomanga.presentation.components.dropdown.SimpleDropdownMenu
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun MangaDetailsHeader(
    generalState: State<MangaConstants.MangaScreenGeneralState>,
    mangaState: State<MangaConstants.MangaScreenMangaState>,
    windowSizeClass: WindowSizeClass,
    isLoggedIntoTrackersProvider: () -> Boolean,
    isSearching: Boolean,
    themeColorState: ThemeColorState,
    generatePalette: (Drawable) -> Unit = {},
    toggleFavorite: () -> Unit = {},
    moveCategories: () -> Unit = {},
    trackingClick: () -> Unit = {},
    artworkClick: () -> Unit = {},
    similarClick: () -> Unit = {},
    mergeClick: () -> Unit = {},
    linksClick: () -> Unit = {},
    shareClick: () -> Unit = {},
    descriptionActions: DescriptionActions,
    informationActions: InformationActions,
    quickReadClick: () -> Unit = {},
) {
    CompositionLocalProvider(
        LocalRippleTheme provides themeColorState.rippleTheme,
        LocalTextSelectionColors provides themeColorState.textSelectionColors) {
            var favoriteExpanded by rememberSaveable { mutableStateOf(false) }

            val isExpanded =
                rememberSaveable(mangaState.value.inLibrary) {
                    when (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                        true -> mutableStateOf(true)
                        false -> mutableStateOf(!mangaState.value.inLibrary)
                    }
                }
            val backdropHeight =
                when (isSearching) {
                    true -> (LocalConfiguration.current.screenHeightDp / 4).dp
                    false -> {
                        when (generalState.value.extraLargeBackdrop) {
                            true -> (LocalConfiguration.current.screenHeightDp / 1.2).dp
                            false -> (LocalConfiguration.current.screenHeightDp / 2.1).dp
                        }
                    }
                }

            Column {
                BoxWithConstraints {
                    BackDrop(
                        themeColorState = themeColorState,
                        artworkProvider = { mangaState.value.currentArtwork },
                        showBackdropProvider = { generalState.value.themeBasedOffCovers },
                        modifier =
                            Modifier.animateContentSize()
                                .fillMaxWidth()
                                .requiredHeightIn(250.dp, maxOf(250.dp, backdropHeight)),
                        generatePalette = generatePalette,
                    )
                    Box(
                        modifier =
                            Modifier.align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.surface),
                                    ),
                                ),
                    )

                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        InformationBlock(
                            themeColorState = themeColorState,
                            titleProvider = { mangaState.value.currentTitle },
                            authorProvider = { mangaState.value.author },
                            artistProvider = { mangaState.value.artist },
                            statsProvider = { mangaState.value.stats },
                            langFlagProvider = { mangaState.value.langFlag },
                            statusProvider = { mangaState.value.status },
                            isPornographicProvider = { mangaState.value.isPornographic },
                            missingChaptersProvider = { mangaState.value.missingChapters },
                            estimatedMissingChapterProvider = {
                                mangaState.value.estimatedMissingChapters
                            },
                            modifier = Modifier.statusBarsPadding().padding(top = 70.dp),
                            isExpandedProvider = { isExpanded.value },
                            showMergedIconProvider = {
                                mangaState.value.isMerged is MergeConstants.IsMergedManga.Yes &&
                                    !generalState.value.hideButtonText
                            },
                            titleLongClick = informationActions.titleLongClick,
                            creatorCopyClick = informationActions.creatorCopy,
                            creatorSearchClick = informationActions.creatorSearch,
                        )
                        AnimatedVisibility(
                            visible = !isSearching,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()) {
                                Column {
                                    Gap(height = 16.dp)
                                    ButtonBlock(
                                        hideButtonTextProvider = {
                                            generalState.value.hideButtonText
                                        },
                                        isInitializedProvider = { mangaState.value.initialized },
                                        isMergedProvider = {
                                            mangaState.value.isMerged is
                                                MergeConstants.IsMergedManga.Yes
                                        },
                                        inLibraryProvider = { mangaState.value.inLibrary },
                                        loggedIntoTrackersProvider = isLoggedIntoTrackersProvider,
                                        trackServiceCountProvider = {
                                            generalState.value.trackServiceCount
                                        },
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
                                    Gap(height = 16.dp)
                                }
                            }
                    }
                }
            }
            AnimatedVisibility(
                visible = !isSearching,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()) {
                    Column {
                        if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                            QuickReadButton(
                                { generalState.value.nextUnreadChapter },
                                themeColorState,
                                quickReadClick)
                        }
                        Gap(Size.tiny)
                        DescriptionBlock(
                            windowSizeClass = windowSizeClass,
                            titleProvider = { mangaState.value.currentTitle },
                            descriptionProvider = { mangaState.value.currentDescription },
                            isInitializedProvider = { mangaState.value.initialized },
                            altTitlesProvider = { mangaState.value.alternativeTitles },
                            genresProvider = { mangaState.value.genres },
                            themeColorState = themeColorState,
                            isExpanded = isExpanded.value,
                            wrapAltTitles = generalState.value.wrapAltTitles,
                            expandCollapseClick = { isExpanded.value = !isExpanded.value },
                            genreSearch = descriptionActions.genreSearch,
                            genreSearchLibrary = descriptionActions.genreSearchLibrary,
                            altTitleClick = descriptionActions.altTitleClick,
                            altTitleResetClick = descriptionActions.altTitleResetClick,
                        )
                        if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
                            QuickReadButton(
                                { generalState.value.nextUnreadChapter },
                                themeColorState,
                                quickReadClick)
                            Gap(Size.tiny)
                        }
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
        Gap(Size.tiny)
        CompositionLocalProvider(
            LocalRippleTheme provides DynamicRippleTheme(themeColorState.altContainerColor)) {
                ElevatedButton(
                    onClick = quickReadClick,
                    shape = RoundedCornerShape(35),
                    modifier = Modifier.fillMaxWidth().padding(Size.small),
                    colors =
                        ButtonDefaults.elevatedButtonColors(
                            containerColor = themeColorState.buttonColor),
                ) {
                    Text(
                        text =
                            stringResource(
                                id = quickReadTextProvider().id!!, quickReadTextProvider().text),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.surface)
                }
            }
    }
}

@Composable
private fun FavoriteDropDown(
    favoriteExpanded: Boolean,
    themeColorState: ThemeColorState,
    moveCategories: () -> Unit,
    toggleFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    SimpleDropdownMenu(
        expanded = favoriteExpanded,
        themeColorState = themeColorState,
        onDismiss = onDismiss,
        dropDownItems =
            persistentListOf(
                SimpleDropDownItem.Action(
                    text = UiText.StringResource(R.string.remove_from_library),
                    onClick = { toggleFavorite() },
                ),
                SimpleDropDownItem.Action(
                    text = UiText.StringResource(R.string.edit_categories),
                    onClick = { moveCategories() },
                ),
            ),
    )
}
