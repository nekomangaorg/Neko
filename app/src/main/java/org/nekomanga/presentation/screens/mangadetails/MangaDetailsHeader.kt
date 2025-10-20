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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DescriptionActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.InformationActions
import eu.kanade.tachiyomi.ui.manga.MergeConstants
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.nekoRippleConfiguration
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun MangaDetailsHeader(
    mangaDetailScreenState: MangaConstants.MangaDetailScreenState,
    windowSizeClass: WindowSizeClass,
    isLoggedIntoTrackers: Boolean,
    themeColorState: ThemeColorState,
    toggleFavorite: () -> Unit,
    onCategoriesClick: () -> Unit,
    onTrackingClick: () -> Unit,
    onArtworkClick: () -> Unit,
    onSimilarClick: () -> Unit,
    onMergeClick: () -> Unit,
    onLinksClick: () -> Unit,
    onShareClick: () -> Unit,
    descriptionActions: DescriptionActions,
    informationActions: InformationActions,
    onQuickReadClick: () -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration,
        LocalTextSelectionColors provides themeColorState.textSelectionColors,
    ) {
        var isDescriptionManuallyExpanded by
            rememberSaveable(mangaDetailScreenState.inLibrary) {
                mutableStateOf(!mangaDetailScreenState.inLibrary)
            }

        val isTablet = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
        val isDescriptionExpanded = isTablet || isDescriptionManuallyExpanded

        // 2. Memoized Calculation: Avoid recalculating backdrop height on every recomposition.
        val screenHeight = LocalConfiguration.current.screenHeightDp
        val backdropHeight =
            remember(
                mangaDetailScreenState.isSearching,
                mangaDetailScreenState.backdropSize,
                screenHeight,
            ) {
                when {
                    mangaDetailScreenState.isSearching -> (screenHeight / 4).dp
                    else ->
                        when (mangaDetailScreenState.backdropSize) {
                            MangaConstants.BackdropSize.Small -> (screenHeight / 2.8).dp
                            MangaConstants.BackdropSize.Large -> (screenHeight / 1.2).dp
                            MangaConstants.BackdropSize.Default -> (screenHeight / 2.1).dp
                        }
                }
            }

        Column {
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                InformationBlock(
                    themeColorState = themeColorState,
                    title = mangaDetailScreenState.currentTitle,
                        author = mangaDetailScreenState.author,
                        artist = mangaDetailScreenState.artist,
                        stats = mangaDetailScreenState.stats,
                        langFlag = mangaDetailScreenState.langFlag,
                        status = mangaDetailScreenState.status,
                        lastChapter =
                            mangaDetailScreenState.lastVolume to mangaDetailScreenState.lastChapter,
                        isPornographic = mangaDetailScreenState.isPornographic,
                        missingChapters = mangaDetailScreenState.missingChapters,
                        estimatedMissingChapters = mangaDetailScreenState.estimatedMissingChapters,
                        isExpanded = isDescriptionExpanded,
                        showMergedIcon =
                            mangaDetailScreenState.isMerged is MergeConstants.IsMergedManga.Yes &&
                                !mangaDetailScreenState.hideButtonText,
                        modifier = Modifier.statusBarsPadding().padding(top = 70.dp),
                        titleLongClick = informationActions.titleLongClick,
                        creatorCopyClick = informationActions.creatorCopy,
                        creatorSearchClick = informationActions.creatorSearch,
                    )

                    AnimatedVisibility(
                        visible = !mangaDetailScreenState.isSearching,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(modifier = Modifier.padding(bottom = Size.medium)) {
                            Gap(Size.medium)
                            ButtonBlock(
                                hideButtonText = mangaDetailScreenState.hideButtonText,
                                isInitialized = mangaDetailScreenState.initialized,
                                isMerged =
                                    mangaDetailScreenState.isMerged
                                        is MergeConstants.IsMergedManga.Yes,
                                inLibrary = mangaDetailScreenState.inLibrary,
                                loggedIntoTrackers = isLoggedIntoTrackers,
                                trackServiceCount = mangaDetailScreenState.trackServiceCount,
                                themeColorState = themeColorState,
                                toggleFavorite = toggleFavorite,
                                trackingClick = onTrackingClick,
                                artworkClick = onArtworkClick,
                                similarClick = onSimilarClick,
                                mergeClick = onMergeClick,
                                linksClick = onLinksClick,
                                shareClick = onShareClick,
                                moveCategories = onCategoriesClick,
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !mangaDetailScreenState.isSearching,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    if (isTablet) {
                        QuickReadButton(
                            quickReadText =
                                mangaDetailScreenState.nextUnreadChapter.text.asString(),
                            themeColorState = themeColorState,
                            onClick = onQuickReadClick,
                        )
                        Gap(Size.tiny)
                    }
                    DescriptionBlock(
                        windowSizeClass = windowSizeClass,
                        title = mangaDetailScreenState.currentTitle,
                        description = mangaDetailScreenState.currentDescription,
                        isInitialized = mangaDetailScreenState.initialized,
                        altTitles = mangaDetailScreenState.alternativeTitles,
                        genres = mangaDetailScreenState.genres,
                        themeColorState = themeColorState,
                        isExpanded = isDescriptionExpanded,
                        wrapAltTitles = mangaDetailScreenState.wrapAltTitles,
                        expandCollapseClick = {
                            isDescriptionManuallyExpanded = !isDescriptionManuallyExpanded
                        },
                        descriptionActions = descriptionActions,
                    )
                    if (!isTablet) {
                        QuickReadButton(
                            quickReadText =
                                mangaDetailScreenState.nextUnreadChapter.text.asString(),
                            themeColorState = themeColorState,
                            onClick = onQuickReadClick,
                        )
                        Gap(Size.tiny)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickReadButton(
    quickReadText: String,
    themeColorState: ThemeColorState,
    onClick: () -> Unit,
) {
    if (quickReadText.isNotBlank()) {
        Spacer(modifier = Modifier.size(Size.tiny))
        CompositionLocalProvider(
            LocalRippleConfiguration provides
                nekoRippleConfiguration(themeColorState.containerColor)
        ) {
            ElevatedButton(
                onClick = onClick,
                shape = ButtonDefaults.shape,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small),
                colors =
                    ButtonDefaults.elevatedButtonColors(
                        containerColor = themeColorState.primaryColor
                    ),
            ) {
                Text(
                    text = quickReadText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.surface,
                )
            }
        }
    }
}
