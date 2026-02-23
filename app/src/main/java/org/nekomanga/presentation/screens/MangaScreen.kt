package org.nekomanga.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.core.content.getSystemService
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.palette.graphics.Palette
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.ui.main.ObserveAsEvents
import eu.kanade.tachiyomi.ui.main.states.RefreshState
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
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import eu.kanade.tachiyomi.util.chapter.isAvailable
import eu.kanade.tachiyomi.util.manga.getSlug
import eu.kanade.tachiyomi.util.storage.getUriWithAuthority
import eu.kanade.tachiyomi.util.system.getBestColor
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.sharedCacheDir
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withUIContext
import java.text.DateFormat
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.snackbar.SnackbarColor
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.VerticalDivider
import org.nekomanga.presentation.components.VerticalFastScroller
import org.nekomanga.presentation.components.dialog.RemovedChaptersDialog
import org.nekomanga.presentation.components.dynamicTextSelectionColor
import org.nekomanga.presentation.components.nekoRippleConfiguration
import org.nekomanga.presentation.components.scaffold.ChildScreenScaffold
import org.nekomanga.presentation.components.snackbar.NekoSnackbarHost
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.extensions.surfaceColorAtElevationCustomColor
import org.nekomanga.presentation.screens.manga.ChapterHeader
import org.nekomanga.presentation.screens.manga.DetailsBottomSheet
import org.nekomanga.presentation.screens.manga.DetailsBottomSheetScreen
import org.nekomanga.presentation.screens.manga.MangaChapterListItem
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
    onSearchMangaDex: (DisplayScreenType) -> Unit,
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    // Re-implementation of ObserveAsEvents from MainActivity
    ObserveAsEvents(flow = mangaViewModel.appSnackbarManager.events, snackbarHostState) { event ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result =
                snackbarHostState.showSnackbar(
                    message = event.getFormattedMessage(context),
                    actionLabel = event.getFormattedActionLabel(context),
                    duration = event.snackbarDuration,
                    withDismissAction = true,
                )
            when (result) {
                SnackbarResult.ActionPerformed -> event.action?.invoke()
                SnackbarResult.Dismissed -> event.dismissAction?.invoke()
            }
        }
    }

    val screenState by mangaViewModel.mangaDetailScreenState.collectAsStateWithLifecycle()

    MangaScreenWrapper(
        screenState = screenState,
        windowSizeClass = windowSizeClass,
        onRefresh = mangaViewModel::onRefresh,
        onSearch = mangaViewModel::onSearch,
        updateSnackbarColor = mangaViewModel::updateSnackbarColor,
        snackbarHost = {
            NekoSnackbarHost(
                snackbarHostState = snackbarHostState,
                snackbarColor = screenState.snackbarColor,
            )
        },
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
                    onSearchMangaDex(DisplayScreenType.AuthorByName(UiText.String(text)))
                },
            ),
        descriptionActions =
            DescriptionActions(
                genreSearch = { text ->
                    onSearchMangaDex(DisplayScreenType.Tag(UiText.String(text)))
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
                markPrevious = mangaViewModel::markPreviousChapters,
            ),
        openWebView = { url, title -> onNavigate(Screens.WebView(title = title, url = url)) },
        onBackPressed = onBackPressed,
    )
}

@Composable
private fun MangaScreenWrapper(
    screenState: MangaConstants.MangaDetailScreenState,
    windowSizeClass: WindowSizeClass,
    onRefresh: () -> Unit,
    onSearch: (String?) -> Unit,
    openWebView: (String, String) -> Unit,
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
    snackbarHost: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val defaultColorState = defaultThemeColorState()

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
        }
    }

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
                    openInWebView = { url, title -> openWebView(url, title) },
                    onNavigate = { newSheet -> scope.launch { currentBottomSheet = newSheet } },
                )
            },
        )
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

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

    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(screenState.initialized, screenState.firstLoad) {
        if (screenState.initialized && !screenState.firstLoad) {
            delay(250L)
            isInitialized = true
        } else {
            isInitialized = false
        }
    }

    val refreshState =
        remember(screenState.isRefreshing, themeColorState) {
            RefreshState(
                enabled = true,
                isRefreshing = screenState.isRefreshing,
                onRefresh = onRefresh,
                trackColor = themeColorState.primaryColor,
            )
        }

    ChildScreenScaffold(
        refreshState = refreshState,
        scrollBehavior = scrollBehavior,
        snackbarHost = snackbarHost,
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
    ) { contentPadding ->
        if (isTablet) {
            SideBySideLayout(
                incomingContentPadding = contentPadding,
                isInitialized = isInitialized,
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
                incomingContentPadding = contentPadding,
                isInitialized = isInitialized,
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
        MangaChapterListItem(
            index = index,
            chapterItem = chapterItem,
            count = chapters.size,
            themeColorState = themeColorState,
            shouldHideChapterTitles =
                screenState.chapterFilter.hideChapterTitles == ToggleableState.On,
            chapterActions = chapterActions,
        )
    }
}

@Composable
private fun VerticalLayout(
    incomingContentPadding: PaddingValues,
    isInitialized: Boolean,
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
    val contentPadding = PaddingValues(bottom = incomingContentPadding.calculateBottomPadding())
    val listState = rememberLazyListState()

    VerticalFastScroller(
        listState = listState,
        modifier = Modifier.fillMaxSize(),
        thumbColor = themeColorState.primaryColor,
        topContentPadding = incomingContentPadding.calculateTopPadding(),
        bottomContentPadding = incomingContentPadding.calculateBottomPadding(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            item(key = "header") {
                MangaDetailsHeader(
                    mangaDetailScreenState = screenState,
                    isInitialized = isInitialized,
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
            if (isInitialized) {
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

@Composable
private fun SideBySideLayout(
    incomingContentPadding: PaddingValues,
    isInitialized: Boolean,
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
        PaddingValues(bottom = incomingContentPadding.calculateBottomPadding())

    val chapterContentPadding =
        PaddingValues(
            bottom = incomingContentPadding.calculateBottomPadding(),
            top = incomingContentPadding.calculateTopPadding(),
        )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(.5f).fillMaxHeight(),
            contentPadding = detailsContentPadding,
        ) {
            item(key = "header") {
                MangaDetailsHeader(
                    mangaDetailScreenState = screenState,
                    isInitialized = isInitialized,
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

        val listState = rememberLazyListState()

        VerticalFastScroller(
            listState = listState,
            modifier =
                Modifier.align(Alignment.TopEnd).fillMaxWidth(.5f).fillMaxHeight().clipToBounds(),
            thumbColor = themeColorState.primaryColor,
            topContentPadding = incomingContentPadding.calculateTopPadding(),
            bottomContentPadding = incomingContentPadding.calculateBottomPadding(),
        ) {
            LazyColumn(state = listState, contentPadding = chapterContentPadding) {
                if (isInitialized) {
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
