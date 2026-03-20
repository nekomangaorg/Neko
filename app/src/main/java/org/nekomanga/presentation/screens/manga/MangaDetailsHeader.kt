package org.nekomanga.presentation.screens.manga

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
    isInitialized: Boolean,
    windowSizeClass: WindowSizeClass,
    isLoggedIntoTrackers: Boolean,
    themeColorState: ThemeColorState,
    generatePalette: (Drawable) -> Unit,
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
            rememberSaveable(mangaDetailScreenState.manga.inLibrary) {
                mutableStateOf(!mangaDetailScreenState.manga.inLibrary)
            }

        val isTablet = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
        val isDescriptionExpanded = isTablet || isDescriptionManuallyExpanded

        val alpha: Float by
            animateFloatAsState(
                targetValue = if (isInitialized) 1f else 0f,
                animationSpec = tween(durationMillis = 600),
            )

        Column {
            Box {
                AnimatedBackdropContainer(
                    isInitialized = isInitialized,
                    themeColorState = themeColorState,
                    dynamicCovers = mangaDetailScreenState.manga.dynamicCovers,
                    artwork = mangaDetailScreenState.manga.currentArtwork,
                    showBackdropOverlay = mangaDetailScreenState.general.themeBasedOffCovers,
                    generatePalette = generatePalette,
                    isSearching = mangaDetailScreenState.general.isSearching,
                    backdropSize = mangaDetailScreenState.general.backdropSize,
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).graphicsLayer(alpha = alpha)
                ) {
                    InformationBlock(
                        themeColorState = themeColorState,
                        title = mangaDetailScreenState.manga.currentTitle,
                        author = mangaDetailScreenState.manga.author,
                        artist = mangaDetailScreenState.manga.artist,
                        stats = mangaDetailScreenState.manga.stats,
                        langFlag = mangaDetailScreenState.manga.langFlag,
                        status = mangaDetailScreenState.manga.status,
                        lastChapter =
                            mangaDetailScreenState.manga.lastVolume to
                                mangaDetailScreenState.manga.lastChapter,
                        isPornographic = mangaDetailScreenState.manga.isPornographic,
                        missingChapters = mangaDetailScreenState.manga.missingChapters,
                        estimatedMissingChapters =
                            mangaDetailScreenState.manga.estimatedMissingChapters,
                        isExpanded = isDescriptionExpanded,
                        showMergedIcon =
                            mangaDetailScreenState.manga.isMerged is
                                MergeConstants.IsMergedManga.Yes &&
                                !mangaDetailScreenState.general.hideButtonText,
                        modifier = Modifier.statusBarsPadding().padding(top = 70.dp),
                        titleLongClick = informationActions.titleLongClick,
                        creatorCopyClick = informationActions.creatorCopy,
                        creatorSearchClick = informationActions.creatorSearch,
                    )

                    AnimatedVisibility(
                        visible = !mangaDetailScreenState.general.isSearching,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(modifier = Modifier.padding(bottom = Size.medium)) {
                            Gap(Size.medium)
                            ButtonBlock(
                                hideButtonText = mangaDetailScreenState.general.hideButtonText,
                                isInitialized = mangaDetailScreenState.manga.initialized,
                                isMerged =
                                    mangaDetailScreenState.manga.isMerged
                                        is MergeConstants.IsMergedManga.Yes,
                                inLibrary = mangaDetailScreenState.manga.inLibrary,
                                loggedIntoTrackers = isLoggedIntoTrackers,
                                trackServiceCount = mangaDetailScreenState.track.trackServiceCount,
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
                visible =
                    mangaDetailScreenState.manga.initialized &&
                        !mangaDetailScreenState.general.isSearching,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    if (isTablet) {
                        QuickReadButton(
                            quickReadText =
                                mangaDetailScreenState.chapters.nextUnreadChapter.text.asString(),
                            themeColorState = themeColorState,
                            onClick = onQuickReadClick,
                        )
                        Gap(Size.tiny)
                    }
                    DescriptionBlock(
                        windowSizeClass = windowSizeClass,
                        title = mangaDetailScreenState.manga.currentTitle,
                        description = mangaDetailScreenState.manga.currentDescription,
                        isInitialized = mangaDetailScreenState.manga.initialized,
                        altTitles = mangaDetailScreenState.manga.alternativeTitles,
                        genres = mangaDetailScreenState.manga.genres,
                        themeColorState = themeColorState,
                        isExpanded = isDescriptionExpanded,
                        wrapAltTitles = mangaDetailScreenState.general.wrapAltTitles,
                        expandCollapseClick = {
                            isDescriptionManuallyExpanded = !isDescriptionManuallyExpanded
                        },
                        descriptionActions = descriptionActions,
                    )
                    if (!isTablet) {
                        QuickReadButton(
                            quickReadText =
                                mangaDetailScreenState.chapters.nextUnreadChapter.text.asString(),
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
        Spacer(modifier = Modifier.size(Size.medium))
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
