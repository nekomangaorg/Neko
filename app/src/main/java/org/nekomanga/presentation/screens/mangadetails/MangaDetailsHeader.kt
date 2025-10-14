package org.nekomanga.presentation.screens.mangadetails

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.LocalRippleConfiguration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DescriptionActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.InformationActions
import eu.kanade.tachiyomi.ui.manga.MergeConstants
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.nekoRippleConfiguration
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun MangaDetailsHeader(
    mangaDetailScreenState: State<MangaConstants.MangaDetailScreenState>,
    windowSizeClass: WindowSizeClass,
    isLoggedIntoTrackers: Boolean,
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
        LocalRippleConfiguration provides themeColorState.rippleConfiguration,
        LocalTextSelectionColors provides themeColorState.textSelectionColors,
    ) {
        var manuallyExpanded by
            rememberSaveable(mangaDetailScreenState.value.inLibrary) {
                mutableStateOf(!mangaDetailScreenState.value.inLibrary)
            }

        val isExpanded =
            windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded || manuallyExpanded

        val backdropHeight =
            when (isSearching) {
                true -> (LocalConfiguration.current.screenHeightDp / 4).dp
                false -> {
                    when (mangaDetailScreenState.value.backdropSize) {
                        MangaConstants.BackdropSize.Small ->
                            (LocalConfiguration.current.screenHeightDp / 2.8).dp
                        MangaConstants.BackdropSize.Default ->
                            (LocalConfiguration.current.screenHeightDp / 2.1).dp
                        MangaConstants.BackdropSize.Large ->
                            (LocalConfiguration.current.screenHeightDp / 1.2).dp
                    }
                }
            }

        Column {
            Box {
                BackDrop(
                    themeColorState = themeColorState,
                    artwork = mangaDetailScreenState.value.currentArtwork,
                    showBackdrop = mangaDetailScreenState.value.themeBasedOffCovers,
                    modifier =
                        Modifier.animateContentSize()
                            .fillMaxWidth()
                            .requiredHeightIn(250.dp, maxOf(250.dp, backdropHeight)),
                    generatePalette = generatePalette,
                )

                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    InformationBlock(
                        themeColorState = themeColorState,
                        titleProvider = { mangaDetailScreenState.value.currentTitle },
                        authorProvider = { mangaDetailScreenState.value.author },
                        artistProvider = { mangaDetailScreenState.value.artist },
                        statsProvider = { mangaDetailScreenState.value.stats },
                        langFlagProvider = { mangaDetailScreenState.value.langFlag },
                        statusProvider = { mangaDetailScreenState.value.status },
                        lastChapterProvider = {
                            mangaDetailScreenState.value.lastVolume to
                                mangaDetailScreenState.value.lastChapter
                        },
                        isPornographicProvider = { mangaDetailScreenState.value.isPornographic },
                        missingChaptersProvider = { mangaDetailScreenState.value.missingChapters },
                        estimatedMissingChapterProvider = {
                            mangaDetailScreenState.value.estimatedMissingChapters
                        },
                        modifier = Modifier.statusBarsPadding().padding(top = 70.dp),
                        isExpandedProvider = { isExpanded },
                        showMergedIconProvider = {
                            mangaDetailScreenState.value.isMerged is
                                MergeConstants.IsMergedManga.Yes &&
                                !mangaDetailScreenState.value.hideButtonText
                        },
                        titleLongClick = informationActions.titleLongClick,
                        creatorCopyClick = informationActions.creatorCopy,
                        creatorSearchClick = informationActions.creatorSearch,
                    )
                    AnimatedVisibility(
                        visible = !isSearching,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            Gap(height = 16.dp)
                            ButtonBlock(
                                hideButtonText = mangaDetailScreenState.value.hideButtonText,
                                isInitialized = mangaDetailScreenState.value.initialized,
                                isMerged =
                                    mangaDetailScreenState.value.isMerged
                                        is MergeConstants.IsMergedManga.Yes,
                                inLibrary = mangaDetailScreenState.value.inLibrary,
                                loggedIntoTrackers = isLoggedIntoTrackers,
                                trackServiceCount = mangaDetailScreenState.value.trackServiceCount,
                                themeColorState = themeColorState,
                                toggleFavorite = toggleFavorite,
                                trackingClick = trackingClick,
                                artworkClick = artworkClick,
                                similarClick = similarClick,
                                mergeClick = mergeClick,
                                linksClick = linksClick,
                                shareClick = shareClick,
                                moveCategories = moveCategories,
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
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                    QuickReadButton(
                        { mangaDetailScreenState.value.nextUnreadChapter },
                        themeColorState,
                        quickReadClick,
                    )
                }
                Gap(Size.tiny)
                DescriptionBlock(
                    windowSizeClass = windowSizeClass,
                    titleProvider = { mangaDetailScreenState.value.currentTitle },
                    description = mangaDetailScreenState.value.currentDescription,
                    isInitializedProvider = { mangaDetailScreenState.value.initialized },
                    altTitlesProvider = { mangaDetailScreenState.value.alternativeTitles },
                    genresProvider = { mangaDetailScreenState.value.genres },
                    themeColorState = themeColorState,
                    isExpanded = isExpanded,
                    wrapAltTitles = mangaDetailScreenState.value.wrapAltTitles,
                    expandCollapseClick = { manuallyExpanded = !manuallyExpanded },
                    genreSearch = descriptionActions.genreSearch,
                    genreSearchLibrary = descriptionActions.genreSearchLibrary,
                    altTitleClick = descriptionActions.altTitleClick,
                    altTitleResetClick = descriptionActions.altTitleResetClick,
                )
                if (windowSizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
                    QuickReadButton(
                        { mangaDetailScreenState.value.nextUnreadChapter },
                        themeColorState,
                        quickReadClick,
                    )
                    Gap(Size.tiny)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.QuickReadButton(
    quickReadTextProvider: () -> MangaConstants.NextUnreadChapter,
    themeColorState: ThemeColorState,
    quickReadClick: () -> Unit,
) {
    val nextChapter = quickReadTextProvider()
    if (nextChapter.text.isNotEmpty() && nextChapter.id != null) {
        Gap(Size.tiny)
        CompositionLocalProvider(
            LocalRippleConfiguration provides
                nekoRippleConfiguration(themeColorState.containerColor)
        ) {
            ElevatedButton(
                onClick = quickReadClick,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().padding(Size.small),
                colors =
                    ButtonDefaults.elevatedButtonColors(
                        containerColor = themeColorState.primaryColor
                    ),
            ) {
                Text(
                    text = stringResource(id = nextChapter.id, nextChapter.text),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.surface,
                )
            }
        }
    }
}
