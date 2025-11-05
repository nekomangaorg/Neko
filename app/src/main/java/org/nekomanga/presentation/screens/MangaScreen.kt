package org.nekomanga.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.palette.graphics.Palette
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.ui.main.states.LocalBarUpdater
import eu.kanade.tachiyomi.ui.main.states.LocalPullRefreshState
import eu.kanade.tachiyomi.ui.main.states.PullRefreshState
import eu.kanade.tachiyomi.ui.main.states.ScreenBars
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CategoryActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.ChapterActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.ChapterFilterActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CoverActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DescriptionActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.InformationActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.MergeActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.TrackActions
import eu.kanade.tachiyomi.ui.manga.MangaViewModel
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.source.browse.BrowseController
import eu.kanade.tachiyomi.ui.source.browse.SearchBrowse
import eu.kanade.tachiyomi.ui.source.browse.SearchType
import eu.kanade.tachiyomi.util.getSlug
import eu.kanade.tachiyomi.util.isAvailable
import eu.kanade.tachiyomi.util.storage.getUriWithAuthority
import eu.kanade.tachiyomi.util.system.getBestColor
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.openInWebView
import eu.kanade.tachiyomi.util.system.sharedCacheDir
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withUIContext
import java.text.DateFormat
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.snackbar.SnackbarColor
import org.nekomanga.presentation.components.ChapterRow
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.VerticalDivider
import org.nekomanga.presentation.components.dialog.RemovedChaptersDialog
import org.nekomanga.presentation.components.dynamicTextSelectionColor
import org.nekomanga.presentation.components.listcard.ExpressiveListCard
import org.nekomanga.presentation.components.listcard.ListCardType
import org.nekomanga.presentation.components.nekoRippleConfiguration
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.extensions.surfaceColorAtElevationCustomColor
import org.nekomanga.presentation.screens.manga.ChapterHeader
import org.nekomanga.presentation.screens.manga.DetailsBottomSheet
import org.nekomanga.presentation.screens.manga.DetailsBottomSheetScreen
import org.nekomanga.presentation.screens.manga.MangaDetailsHeader
import org.nekomanga.presentation.screens.manga.MangaScreenTopBar
import org.nekomanga.presentation.theme.Size

@Composable
fun MangaScreen(
    mangaViewModel: MangaViewModel,
    windowSizeClass: WindowSizeClass,
    onBackPressed: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    onSearchLibrary: (String) -> Unit,
    onSearchMangaDex: (SearchBrowse) -> Unit,
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    MangaScreenWrapper(
        screenState = mangaViewModel.mangaDetailScreenState.collectAsStateWithLifecycle().value,
        windowSizeClass = windowSizeClass,
        onRefresh = mangaViewModel::onRefresh,
        onSearch = mangaViewModel::onSearch,
        updateSnackbarColor = mangaViewModel::updateSnackbarColor,
        categoryActions =
            CategoryActions(
                set = { enabledCategories ->
                    mangaViewModel.updateMangaCategories(enabledCategories)
                },
                addNew = { newCategory -> mangaViewModel.addNewCategory(newCategory) },
            ),
        informationActions =
            InformationActions(
                titleLongClick = {
                    mangaViewModel.copiedToClipboard(it)
                    copyToClipboard(context, it, R.string.title)
                },
                creatorCopy = {
                    mangaViewModel.copiedToClipboard(it)
                    copyToClipboard(context, it, R.string.creator)
                },
                creatorSearch = { text ->
                    onSearchMangaDex(SearchBrowse(query = text, type = SearchType.Creator))
                },
            ),
        descriptionActions =
            DescriptionActions(
                genreSearch = { text ->
                    onSearchMangaDex(SearchBrowse(query = text, type = SearchType.Tag))
                },
                genreSearchLibrary = onSearchLibrary,
                altTitleClick = mangaViewModel::setAltTitle,
                altTitleResetClick = { mangaViewModel.setAltTitle(null) },
            ),
        generatePalette = { drawable ->
            val bitmap = drawable.toBitmap()
            Palette.from(bitmap).generate {
                it ?: return@generate
                scope.launch {
                    val vibrantColor = it.getBestColor() ?: return@launch
                    mangaViewModel.updateMangaColor(vibrantColor)
                }
            }
        },
        onToggleFavorite = mangaViewModel::toggleFavorite,
        dateFormat = mangaViewModel.preferences.dateFormat(),
        trackActions =
            TrackActions(
                statusChange = { statusIndex, trackAndService ->
                    mangaViewModel.updateTrackStatus(statusIndex, trackAndService)
                },
                scoreChange = { statusIndex, trackAndService ->
                    mangaViewModel.updateTrackScore(statusIndex, trackAndService)
                },
                chapterChange = { newChapterNumber, trackAndService ->
                    mangaViewModel.updateTrackChapter(newChapterNumber, trackAndService)
                },
                search = { title, service -> mangaViewModel.searchTracker(title, service) },
                searchItemClick = { trackAndService ->
                    mangaViewModel.registerTracking(trackAndService)
                },
                remove = { alsoRemoveFromTracker, service ->
                    mangaViewModel.removeTracking(alsoRemoveFromTracker, service)
                },
                dateChange = { trackDateChange -> mangaViewModel.updateTrackDate(trackDateChange) },
            ),
        mergeActions =
            MergeActions(
                remove = mangaViewModel::removeMergedManga,
                search = mangaViewModel::searchMergedManga,
                add = mangaViewModel::addMergedManga,
            ),
        onSimilarClick = { onNavigate(Screens.Similar(mangaViewModel.getManga().uuid())) },
        onShareClick = {
            scope.launch {
                val dir = context.sharedCacheDir() ?: throw Exception("Error accessing cache dir")

                val cover =
                    mangaViewModel.shareCover(
                        dir,
                        mangaViewModel.mangaDetailScreenState.value.currentArtwork,
                    )
                val manga = mangaViewModel.getManga()
                val sharableCover = cover?.getUriWithAuthority(context)

                withUIContext {
                    try {
                        var url =
                            mangaViewModel.sourceManager.mangaDex
                                .mangaDetailsRequest(manga)
                                .url
                                .toString()
                        url = "$url/" + manga.getSlug()
                        val intent =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/*"
                                putExtra(Intent.EXTRA_TEXT, url)
                                putExtra(Intent.EXTRA_TITLE, manga.title)
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                if (cover != null) {
                                    clipData = ClipData.newRawUri(null, sharableCover)
                                }
                            }
                        context.startActivity(
                            Intent.createChooser(intent, context.getString(R.string.share))
                        )
                    } catch (e: Exception) {
                        context.toast(e.message)
                    }
                }
            }
        },
        coverActions =
            CoverActions(
                share = { _, artwork ->
                    scope.launch {
                        val dir =
                            context.sharedCacheDir() ?: throw Exception("Error accessing cache dir")
                        val cover = mangaViewModel.shareCover(dir, artwork)
                        val sharableCover = cover?.getUriWithAuthority(context)
                        withUIContext {
                            try {
                                val intent =
                                    Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_STREAM, sharableCover)
                                        flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        clipData = ClipData.newRawUri(null, sharableCover)
                                        type = "image/*"
                                    }
                                context.startActivity(
                                    Intent.createChooser(intent, context.getString(R.string.share))
                                )
                            } catch (e: Exception) {
                                context.toast(e.message)
                            }
                        }
                    }
                },
                set = mangaViewModel::setCover,
                save = mangaViewModel::saveCover,
                reset = mangaViewModel::resetCover,
            ),
        chapterFilterActions =
            ChapterFilterActions(
                changeSort = mangaViewModel::changeSortOption,
                changeFilter = mangaViewModel::changeFilterOption,
                changeScanlator = mangaViewModel::changeScanlatorOption,
                changeLanguage = mangaViewModel::changeLanguageOption,
                setAsGlobal = mangaViewModel::setGlobalOption,
            ),
        chapterActions =
            ChapterActions(
                mark = mangaViewModel::markChapters,
                download = { chapterItems, downloadAction ->
                    if (
                        chapterItems.size == 1 &&
                            MdConstants.UnsupportedOfficialGroupList.contains(
                                chapterItems[0].chapter.scanlator
                            )
                    ) {
                        context.toast(
                            "${chapterItems[0].chapter.scanlator} not supported, try WebView"
                        )
                    } else {
                        mangaViewModel.downloadChapters(chapterItems, downloadAction)
                    }
                },
                delete = mangaViewModel::deleteChapters,
                clearRemoved = mangaViewModel::clearRemovedChapters,
                openNext = {
                    mangaViewModel.mangaDetailScreenState.value.nextUnreadChapter.simpleChapter
                        ?.let { simpleChapter ->
                            val chapter = simpleChapter.toDbChapter()
                            val manga = mangaViewModel.getManga()
                            if (
                                chapter.scanlator != null &&
                                    MdConstants.UnsupportedOfficialGroupList.contains(
                                        chapter.scanlator
                                    )
                            ) {
                                context.toast("${chapter.scanlator} not supported, try WebView")
                            } else if (
                                !chapter.isAvailable(mangaViewModel.downloadManager, manga)
                            ) {
                                context.toast("Chapter is not available")
                            } else {
                                context.startActivity(
                                    ReaderActivity.newIntent(context, manga, chapter)
                                )
                            }
                        }
                },
                open = { chapterItem ->
                    val chapter = chapterItem.chapter.toDbChapter()
                    val manga = mangaViewModel.getManga()
                    if (
                        chapter.scanlator != null &&
                            MdConstants.UnsupportedOfficialGroupList.contains(chapter.scanlator)
                    ) {
                        context.toast("${chapter.scanlator} not supported, try WebView")
                    } else if (!chapter.isAvailable(mangaViewModel.downloadManager, manga)) {
                        context.toast("Chapter is not available")
                    } else {
                        context.startActivity(ReaderActivity.newIntent(context, manga, chapter))
                    }
                },
                blockScanlator = mangaViewModel::blockScanlator,
                openComment = { chapterId -> mangaViewModel.openComment(context, chapterId) },
                createMangaFolder = mangaViewModel::createMangaFolder,
                openInBrowser = { chapterItem ->
                    if (chapterItem.chapter.isUnavailable) {
                        context.toast("Chapter is not available")
                    } else {
                        val url = mangaViewModel.getChapterUrl(chapterItem.chapter)
                        context.openInBrowser(url)
                    }
                },
            ),
        onBackPressed = onBackPressed,
    )
}

@Composable
private fun MangaScreenWrapper(
    screenState: MangaConstants.MangaDetailScreenState,
    windowSizeClass: WindowSizeClass,
    onRefresh: () -> Unit,
    onSearch: (String?) -> Unit,
    generatePalette: (Drawable) -> Unit,
    updateSnackbarColor: (SnackbarColor) -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    categoryActions: CategoryActions,
    dateFormat: DateFormat,
    trackActions: TrackActions,
    onSimilarClick: () -> Unit,
    coverActions: CoverActions,
    mergeActions: MergeActions,
    onShareClick: () -> Unit,
    informationActions: InformationActions,
    descriptionActions: DescriptionActions,
    chapterFilterActions: ChapterFilterActions,
    chapterActions: ChapterActions,
    onBackPressed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val defaultColorState = defaultThemeColorState()

    val updateTopBar = LocalBarUpdater.current
    val updateRefreshState = LocalPullRefreshState.current

    val pullRefreshState = remember { PullRefreshState() }

    val themeColorState =
        remember(screenState.themeBasedOffCovers, screenState.vibrantColor, isDark) {
            // 3. The logic inside here is now non-composable.
            if (screenState.themeBasedOffCovers && screenState.vibrantColor != null) {
                val color = getButtonThemeColor(Color(screenState.vibrantColor), isDark)
                val containerColor =
                    Color(ColorUtils.blendARGB(color.toArgb(), surfaceColor.toArgb(), .706f))
                val onContainerColor =
                    Color(ColorUtils.blendARGB(color.toArgb(), onSurfaceColor.toArgb(), .706f))

                ThemeColorState(
                    primaryColor = color,
                    rippleColor = color.copy(alpha = NekoColors.mediumAlphaLowContrast),
                    rippleConfiguration = nekoRippleConfiguration(color),
                    textSelectionColors = dynamicTextSelectionColor(color),
                    containerColor = containerColor,
                    onContainerColor = onContainerColor,
                    altContainerColor =
                        surfaceColor.surfaceColorAtElevationCustomColor(
                            surfaceColor,
                            color,
                            Size.small,
                        ),
                    onAltContainerColor = color,
                )
            } else {
                defaultColorState
            }
        }

    LaunchedEffect(themeColorState) {
        updateSnackbarColor(
            SnackbarColor(
                actionColor = themeColorState.primaryColor,
                containerColor = themeColorState.containerColor,
                contentColor = themeColorState.onContainerColor,
            )
        )
    }

    var currentBottomSheet by remember { mutableStateOf<DetailsBottomSheetScreen?>(null) }

    LaunchedEffect(currentBottomSheet) {
        if (currentBottomSheet != null) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    BackHandler(enabled = currentBottomSheet != null) { currentBottomSheet = null }

    fun openSheet(sheet: DetailsBottomSheetScreen) {
        scope.launch { currentBottomSheet = sheet }
    }

    if (currentBottomSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { scope.launch { currentBottomSheet = null } },
            sheetState = sheetState,
            content = {
                DetailsBottomSheet(
                    currentScreen = currentBottomSheet!!,
                    themeColorState = themeColorState,
                    mangaDetailScreenState = screenState,
                    addNewCategory = categoryActions.addNew,
                    dateFormat = dateFormat,
                    trackActions = trackActions,
                    coverActions = coverActions,
                    mergeActions = mergeActions,
                    chapterFilterActions = chapterFilterActions,
                    openInWebView = { url, title -> context.openInWebView(url, title) },
                    onNavigate = { newSheet -> scope.launch { currentBottomSheet = newSheet } },
                )
            },
        )
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val screenBars = remember {
        ScreenBars(
            topBar = {
                MangaScreenTopBar(
                    screenState = screenState,
                    chapterActions = chapterActions,
                    themeColorState = themeColorState,
                    scrollBehavior = scrollBehavior,
                    onNavigationIconClick = onBackPressed,
                    onSearch = onSearch,
                )
            },
            scrollBehavior = scrollBehavior,
        )
    }
    DisposableEffect(Unit) {
        updateTopBar(screenBars)
        onDispose { updateTopBar(ScreenBars(id = screenBars.id, topBar = null)) }
    }

    DisposableEffect(screenState.isRefreshing, onRefresh, themeColorState) {
        updateRefreshState(
            pullRefreshState.copy(
                enabled = true,
                isRefreshing = screenState.isRefreshing,
                onRefresh = onRefresh,
                trackColor = themeColorState.primaryColor,
            )
        )
        onDispose { updateRefreshState(pullRefreshState.copy(onRefresh = null)) }
    }

    val isTablet =
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded &&
            !screenState.forcePortrait

    val onToggleFavoriteAction =
        remember(screenState.inLibrary, screenState.allCategories, screenState.hasDefaultCategory) {
            {
                if (!screenState.inLibrary && screenState.allCategories.isNotEmpty()) {
                    if (screenState.hasDefaultCategory) {
                        onToggleFavorite(true)
                    } else {
                        openSheet(
                            DetailsBottomSheetScreen.CategoriesSheet(
                                addingToLibrary = true,
                                setCategories = categoryActions.set,
                                addToLibraryClick = { onToggleFavorite(false) },
                            )
                        )
                    }
                } else {
                    onToggleFavorite(false)
                }
            }
        }

    val screenHeight = LocalConfiguration.current.screenHeightDp
    val backdropHeight by
        animateDpAsState(
            targetValue =
                when {
                    !screenState.initialized -> screenHeight.dp
                    screenState.isSearching -> (screenHeight / 4).dp
                    else ->
                        when (screenState.backdropSize) {
                            MangaConstants.BackdropSize.Small -> (screenHeight / 2.8).dp
                            MangaConstants.BackdropSize.Large -> (screenHeight / 1.2).dp
                            MangaConstants.BackdropSize.Default -> (screenHeight / 2.1).dp
                        }
                }
        )

    if (isTablet) {
        SideBySideLayout(
            incomingPadding = PaddingValues(),
            backdropHeight = backdropHeight,
            screenState = screenState,
            windowSizeClass = windowSizeClass,
            themeColorState = themeColorState,
            chapterActions = chapterActions,
            informationActions = informationActions,
            descriptionActions = descriptionActions,
            onSimilarClick = onSimilarClick,
            onShareClick = onShareClick,
            onToggleFavorite = onToggleFavoriteAction,
            generatePalette = generatePalette,
            onOpenSheet = ::openSheet,
            categoryActions = categoryActions,
        )
    } else {
        VerticalLayout(
            incomingPadding = PaddingValues(),
            backdropHeight = backdropHeight,
            screenState = screenState,
            windowSizeClass = windowSizeClass,
            themeColorState = themeColorState,
            chapterActions = chapterActions,
            informationActions = informationActions,
            descriptionActions = descriptionActions,
            onSimilarClick = onSimilarClick,
            onShareClick = onShareClick,
            onToggleFavorite = onToggleFavoriteAction,
            generatePalette = generatePalette,
            onOpenSheet = ::openSheet,
            categoryActions = categoryActions,
        )
    }

    if (screenState.removedChapters.isNotEmpty()) {
        RemovedChaptersDialog(
            themeColorState = themeColorState,
            chapters = screenState.removedChapters,
            onConfirm = {
                chapterActions.delete(screenState.removedChapters)
                chapterActions.clearRemoved()
            },
            onDismiss = { chapterActions.clearRemoved() },
        )
    }
}

private fun LazyListScope.chapterList(
    chapters: PersistentList<ChapterItem>,
    screenState: MangaConstants.MangaDetailScreenState,
    themeColorState: ThemeColorState,
    chapterActions: ChapterActions,
    onOpenSheet: (DetailsBottomSheetScreen) -> Unit,
) {
    item(key = "chapter_header") {
        ChapterHeader(
            themeColor = themeColorState,
            numberOfChapters = chapters.size,
            filterText = screenState.chapterFilterText,
            onClick = { onOpenSheet(DetailsBottomSheetScreen.FilterChapterSheet) },
        )
    }

    itemsIndexed(items = chapters, key = { _, chapter -> chapter.chapter.id }) { index, chapterItem
        ->
        val listCardType =
            when {
                index == 0 && chapters.size > 1 -> ListCardType.Top
                index == chapters.size - 1 && chapters.size > 1 -> ListCardType.Bottom

                chapters.size == 1 -> ListCardType.Single
                else -> ListCardType.Center
            }
        ExpressiveListCard(
            modifier = Modifier.padding(horizontal = Size.small),
            listCardType = listCardType,
            themeColorState = themeColorState,
        ) {
            ChapterRow(
                themeColor = themeColorState,
                chapterItem = chapterItem,
                shouldHideChapterTitles =
                    screenState.chapterFilter.hideChapterTitles == ToggleableState.On,
                onClick = { chapterActions.open(chapterItem) },
                onBookmark = {
                    chapterActions.mark(
                        listOf(chapterItem),
                        if (chapterItem.chapter.bookmark) ChapterMarkActions.UnBookmark(true)
                        else ChapterMarkActions.Bookmark(true),
                    )
                },
                onRead = {
                    chapterActions.mark(
                        listOf(chapterItem),
                        if (chapterItem.chapter.read) ChapterMarkActions.Unread(true)
                        else ChapterMarkActions.Read(true),
                    )
                },
                onWebView = { chapterActions.openInBrowser(chapterItem) },
                onComment = { chapterActions.openComment(chapterItem.chapter.mangaDexChapterId) },
                onDownload = { downloadAction ->
                    chapterActions.download(listOf(chapterItem), downloadAction)
                },
                markPrevious = { read ->
                    val chaptersToMark = screenState.activeChapters.subList(0, index)
                    val altChapters =
                        if (index == screenState.activeChapters.lastIndex) emptyList()
                        else
                            screenState.activeChapters.slice(
                                index + 1..screenState.activeChapters.lastIndex
                            )
                    val action =
                        if (read) ChapterMarkActions.PreviousRead(true, altChapters)
                        else ChapterMarkActions.PreviousUnread(true, altChapters)
                    chapterActions.mark(chaptersToMark, action)
                },
                blockScanlator = { blockType, blocked ->
                    chapterActions.blockScanlator(blockType, blocked)
                },
            )
        }
        if (listCardType != ListCardType.Bottom) {
            Gap(Size.tiny)
        }
    }
}

@Composable
private fun VerticalLayout(
    incomingPadding: PaddingValues,
    backdropHeight: Dp,
    screenState: MangaConstants.MangaDetailScreenState,
    windowSizeClass: WindowSizeClass,
    themeColorState: ThemeColorState,
    categoryActions: CategoryActions,
    chapterActions: ChapterActions,
    informationActions: InformationActions,
    descriptionActions: DescriptionActions,
    onSimilarClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    generatePalette: (Drawable) -> Unit,
    onOpenSheet: (DetailsBottomSheetScreen) -> Unit,
) {
    val contentPadding =
        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "header") {
            MangaDetailsHeader(
                mangaDetailScreenState = screenState,
                backdropHeight = backdropHeight,
                windowSizeClass = windowSizeClass,
                isLoggedIntoTrackers = screenState.loggedInTrackService.isNotEmpty(),
                themeColorState = themeColorState,
                generatePalette = generatePalette,
                toggleFavorite = onToggleFavorite,
                onCategoriesClick = {
                    onOpenSheet(
                        DetailsBottomSheetScreen.CategoriesSheet(
                            setCategories = categoryActions.set
                        )
                    )
                },
                onTrackingClick = { onOpenSheet(DetailsBottomSheetScreen.TrackingSheet) },
                onArtworkClick = { onOpenSheet(DetailsBottomSheetScreen.ArtworkSheet) },
                onSimilarClick = onSimilarClick,
                onMergeClick = { onOpenSheet(DetailsBottomSheetScreen.MergeSheet) },
                onLinksClick = { onOpenSheet(DetailsBottomSheetScreen.ExternalLinksSheet) },
                onShareClick = onShareClick,
                descriptionActions = descriptionActions,
                informationActions = informationActions,
                onQuickReadClick = { chapterActions.openNext() },
            )
        }
        if (screenState.initialized) {
            chapterList(
                chapters =
                    if (screenState.isSearching) screenState.searchChapters
                    else screenState.activeChapters,
                screenState = screenState,
                themeColorState = themeColorState,
                chapterActions = chapterActions,
                onOpenSheet = onOpenSheet,
            )
        }
    }
}

@Composable
private fun SideBySideLayout(
    incomingPadding: PaddingValues,
    backdropHeight: Dp,
    screenState: MangaConstants.MangaDetailScreenState,
    windowSizeClass: WindowSizeClass,
    themeColorState: ThemeColorState,
    categoryActions: CategoryActions,
    chapterActions: ChapterActions,
    informationActions: InformationActions,
    descriptionActions: DescriptionActions,
    onSimilarClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    generatePalette: (Drawable) -> Unit,
    onOpenSheet: (DetailsBottomSheetScreen) -> Unit,
) {
    val detailsContentPadding =
        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues()
    val chapterContentPadding =
        PaddingValues(
            bottom = detailsContentPadding.calculateBottomPadding(),
            top = incomingPadding.calculateTopPadding(),
        )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(.5f).fillMaxHeight(),
            contentPadding = detailsContentPadding,
        ) {
            item(key = "header") {
                MangaDetailsHeader(
                    mangaDetailScreenState = screenState,
                    backdropHeight = backdropHeight,
                    windowSizeClass = windowSizeClass,
                    isLoggedIntoTrackers = screenState.loggedInTrackService.isNotEmpty(),
                    themeColorState = themeColorState,
                    generatePalette = generatePalette,
                    toggleFavorite = onToggleFavorite,
                    onCategoriesClick = {
                        onOpenSheet(
                            DetailsBottomSheetScreen.CategoriesSheet(
                                setCategories = categoryActions.set
                            )
                        )
                    },
                    onTrackingClick = { onOpenSheet(DetailsBottomSheetScreen.TrackingSheet) },
                    onArtworkClick = { onOpenSheet(DetailsBottomSheetScreen.ArtworkSheet) },
                    onSimilarClick = onSimilarClick,
                    onMergeClick = { onOpenSheet(DetailsBottomSheetScreen.MergeSheet) },
                    onLinksClick = { onOpenSheet(DetailsBottomSheetScreen.ExternalLinksSheet) },
                    onShareClick = onShareClick,
                    descriptionActions = descriptionActions,
                    informationActions = informationActions,
                    onQuickReadClick = { chapterActions.openNext() },
                )
            }
        }

        VerticalDivider(Modifier.align(Alignment.TopCenter))

        LazyColumn(
            modifier =
                Modifier.align(Alignment.TopEnd).fillMaxWidth(.5f).fillMaxHeight().clipToBounds(),
            contentPadding = chapterContentPadding,
        ) {
            if (screenState.initialized) {
                chapterList(
                    chapters =
                        if (screenState.isSearching) screenState.searchChapters
                        else screenState.activeChapters,
                    screenState = screenState,
                    themeColorState = themeColorState,
                    chapterActions = chapterActions,
                    onOpenSheet = onOpenSheet,
                )
            }
        }
    }
}

private fun getButtonThemeColor(buttonColor: Color, isNightMode: Boolean): Color {
    val color1 = buttonColor.toArgb()
    val luminance = ColorUtils.calculateLuminance(color1).toFloat()

    val color2 =
        when (isNightMode) {
            true -> Color.White.toArgb()
            false -> Color.Black.toArgb()
        }

    val ratio =
        when (isNightMode) {
            true -> (-(luminance - 1)) * .33f
            false -> luminance * .3f
        }

    return when ((isNightMode && luminance <= 0.6) || (!isNightMode && luminance > 0.4)) {
        true -> Color(ColorUtils.blendARGB(color1, color2, ratio))
        false -> buttonColor
    }
}

/** Copy the Device and App info to clipboard */
private fun copyToClipboard(context: Context, content: String, @StringRes label: Int) {
    val clipboard = context.getSystemService<ClipboardManager>()!!
    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(label), content))
}

private fun getBrowseController(backstackNumber: Int = 2): BrowseController? {
    /* val position = router.backstackSize - backstackNumber
    if (position < 0) return null
    return when (val previousController = router.backstack[position].controller) {
        is LibraryController,
        is DisplayController,
        is SimilarController -> {
            router.popToRoot()
            (activity as? MainActivity)?.goToTab(R.id.nav_browse)
            router.getControllerWithTag(R.id.nav_browse.toString()) as BrowseController
        }
        is BrowseController -> {
            router.popCurrentController()
            previousController
        }
        else -> {
            if (backstackNumber == 1) {
                null
            } else {
                getBrowseController(backstackNumber - 1)
            }
        }
    }*/
    return null
}
