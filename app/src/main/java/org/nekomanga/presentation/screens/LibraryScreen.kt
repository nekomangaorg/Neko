package org.nekomanga.presentation.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.library.LibraryCategoryActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenActions
import eu.kanade.tachiyomi.ui.library.LibrarySheetActions
import eu.kanade.tachiyomi.ui.library.LibraryViewModel
import eu.kanade.tachiyomi.ui.main.LocalBarUpdater
import eu.kanade.tachiyomi.ui.main.LocalPullRefreshState
import eu.kanade.tachiyomi.ui.main.PullRefreshState
import eu.kanade.tachiyomi.ui.main.ScreenBars
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dialog.ConfirmationDialog
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.screens.library.LibraryBottomSheet
import org.nekomanga.presentation.screens.library.LibraryBottomSheetScreen
import org.nekomanga.presentation.screens.library.LibraryPage
import org.nekomanga.presentation.screens.library.LibraryScreenTopBar
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel,
    windowSizeClass: WindowSizeClass,
    incomingContentPadding: PaddingValues = PaddingValues(),
) {

    LibraryWrapper(
        libraryViewModel = libraryViewModel,
        incomingContentPadding = incomingContentPadding,
        libraryScreenActions =
            LibraryScreenActions(
                mangaClick = { /*::openManga*/ },
                mangaLongClick = libraryViewModel::libraryItemLongClick,
                selectAllLibraryMangaItems = libraryViewModel::selectAllLibraryMangaItems,
                deleteSelectedLibraryMangaItems = libraryViewModel::deleteSelectedLibraryMangaItems,
                clearSelectedManga = libraryViewModel::clearSelectedManga,
                search = libraryViewModel::search,
                searchMangaDex = { /* ::searchMangaDex,*/ },
                updateLibrary = { /*updateLibrary(context)*/ },
                collapseExpandAllCategories = libraryViewModel::collapseExpandAllCategories,
                clearActiveFilters = libraryViewModel::clearActiveFilters,
                filterToggled = libraryViewModel::filterToggled,
                downloadChapters = libraryViewModel::downloadChapters,
                shareManga = { /* shareManga(context)*/ },
                markMangaChapters = libraryViewModel::markChapters,
                syncMangaToDex = libraryViewModel::syncMangaToDex,
                mangaStartReadingClick = { mangaId ->
                    libraryViewModel.openNextUnread(
                        mangaId,
                        { manga, chapter ->
                            /*startActivity(
                                ReaderActivity.newIntent(
                                    context,
                                    manga,
                                    chapter,
                                )
                            )*/
                        },
                    )
                },
            ),
        librarySheetActions =
            LibrarySheetActions(
                groupByClick = libraryViewModel::groupByClick,
                categoryItemLibrarySortClick = libraryViewModel::categoryItemLibrarySortClick,
                libraryDisplayModeClick = libraryViewModel::libraryDisplayModeClick,
                rawColumnCountChanged = libraryViewModel::rawColumnCountChanged,
                outlineCoversToggled = libraryViewModel::outlineCoversToggled,
                downloadBadgesToggled = libraryViewModel::downloadBadgesToggled,
                unreadBadgesToggled = libraryViewModel::unreadBadgesToggled,
                startReadingButtonToggled = libraryViewModel::startReadingButtonToggled,
                horizontalCategoriesToggled = libraryViewModel::horizontalCategoriesToggled,
                showLibraryButtonBarToggled = libraryViewModel::showLibraryButtonBarToggled,
                editCategories = libraryViewModel::editCategories,
                addNewCategory = libraryViewModel::addNewCategory,
            ),
        libraryCategoryActions =
            LibraryCategoryActions(
                categoryItemClick = libraryViewModel::categoryItemClick,
                categoryAscendingClick = libraryViewModel::categoryAscendingClick,
                categoryRefreshClick = { /*category -> updateCategory(category, context)*/ },
            ),
        windowSizeClass = windowSizeClass,
        settingsClick = {},
        incognitoClick = {},
        statsClick = {},
        aboutClick = {},
        helpClick = {},
    )
}

@Composable
private fun LibraryWrapper(
    libraryViewModel: LibraryViewModel,
    libraryScreenActions: LibraryScreenActions,
    librarySheetActions: LibrarySheetActions,
    libraryCategoryActions: LibraryCategoryActions,
    windowSizeClass: WindowSizeClass,
    incomingContentPadding: PaddingValues = PaddingValues(),
    incognitoClick: () -> Unit,
    settingsClick: () -> Unit,
    statsClick: () -> Unit,
    helpClick: () -> Unit,
    aboutClick: () -> Unit,
) {

    val libraryScreenState by libraryViewModel.libraryScreenState.collectAsState()

    val updateTopBar = LocalBarUpdater.current
    val updateRefreshState = LocalPullRefreshState.current

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var mainDropdownShowing by remember { mutableStateOf(false) }

    var selectionMode by
        remember(libraryScreenState.selectedItems) {
            mutableStateOf(libraryScreenState.selectedItems.isNotEmpty())
        }

    var deleteMangaConfirmation by remember { mutableStateOf(false) }

    var markActionConfirmation by remember { mutableStateOf<ChapterMarkActions?>(null) }

    var removeActionConfirmation by remember {
        mutableStateOf<MangaConstants.DownloadAction?>(null)
    }

    var currentBottomSheet: LibraryBottomSheetScreen? by remember { mutableStateOf(null) }

    LaunchedEffect(currentBottomSheet) {
        if (currentBottomSheet != null) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    /** Close the bottom sheet on back if its open */
    BackHandler(enabled = currentBottomSheet != null || selectionMode) {
        if (currentBottomSheet != null) {
            currentBottomSheet == null
        } else {
            libraryScreenActions.clearSelectedManga()
        }
    }

    // set the current sheet to null when bottom sheet is closed
    LaunchedEffect(key1 = sheetState.isVisible) {
        if (!sheetState.isVisible) {
            currentBottomSheet = null
        }
    }

    val openSheet: (LibraryBottomSheetScreen) -> Unit = { scope.launch { currentBottomSheet = it } }

    Box(
        modifier =
            Modifier.fillMaxSize().conditional(mainDropdownShowing) {
                this.blur(Size.medium).clickable(enabled = false) {}
            }
    ) {
        if (currentBottomSheet != null) {

            ModalBottomSheet(
                sheetState = sheetState,
                shape =
                    RoundedCornerShape(topStart = Shapes.sheetRadius, topEnd = Shapes.sheetRadius),
                onDismissRequest = { currentBottomSheet = null },
                content = {
                    Box(modifier = Modifier.defaultMinSize(minHeight = Size.extraExtraTiny)) {
                        currentBottomSheet?.let { currentSheet ->
                            LibraryBottomSheet(
                                libraryScreenState = libraryScreenState,
                                librarySheetActions = librarySheetActions,
                                currentScreen = currentSheet,
                                closeSheet = { scope.launch { currentBottomSheet = null } },
                            )
                        }
                    }
                },
            )
        }

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

        DisposableEffect(Unit) {
            updateTopBar(
                ScreenBars(
                    topBar = {
                        LibraryScreenTopBar(
                            scrollBehavior = scrollBehavior,
                            libraryScreenState = libraryScreenState,
                            libraryScreenActions = libraryScreenActions,
                            groupByClick = {
                                scope.launch { openSheet(LibraryBottomSheetScreen.GroupBySheet) }
                            },
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            )
            onDispose { updateTopBar(ScreenBars()) }
        }

        DisposableEffect(libraryScreenState.isRefreshing, libraryScreenActions.updateLibrary) {
            updateRefreshState(
                PullRefreshState(
                    enabled = true,
                    isRefreshing = libraryScreenState.isRefreshing,
                    onRefresh = libraryScreenActions.updateLibrary,
                )
            )
            onDispose { updateRefreshState(PullRefreshState()) }
        }

        /* NekoScaffold(
        type =
            if (selectionMode) NekoScaffoldType.TitleAndSubtitle
            else NekoScaffoldType.SearchOutlineWithActions,
        title =
            if (selectionMode) "Selected: ${libraryScreenState.selectedItems.size}"
            else "",
        searchPlaceHolder = stringResource(R.string.search_library),
        searchPlaceHolderAlt = stringResource(R.string.library_search_hint),
        incognitoMode = libraryScreenState.incognitoMode,
        isRoot = true,
        onNavigationIconClicked = {
            if (selectionMode) {
                libraryScreenActions.clearSelectedManga()
            }
        },
        onSearch = libraryScreenActions.search,
        altAppBarColor = selectionMode,
        actions = {
            if (selectionMode) {
                LibraryAppBarActions(
                    downloadChapters = libraryScreenActions.downloadChapters,
                    removeDownloads = { removeAction ->
                        removeActionConfirmation = removeAction
                    },
                    shareManga = libraryScreenActions.shareManga,
                    syncMangaToDexClick = libraryScreenActions.syncMangaToDex,
                    editCategoryClick = {
                        scope.launch { openSheet(LibraryBottomSheetScreen.CategorySheet) }
                    },
                    markChapterClick = { markAction ->
                        markActionConfirmation = markAction
                    },
                    removeFromLibraryClick = { deleteMangaConfirmation = true },
                )
            } else {
                AppBarActions(
                    actions =
                        listOf(
                            AppBar.Action(
                                title = UiText.StringResource(R.string.settings),
                                icon = Icons.Outlined.Tune,
                                onClick = {
                                    scope.launch {
                                        openSheet(
                                            LibraryBottomSheetScreen.DisplayOptionsSheet
                                        )
                                    }
                                },
                            ),
                            AppBar.MainDropdown(
                                incognitoMode = libraryScreenState.incognitoMode,
                                incognitoModeClick = incognitoClick,
                                settingsClick = settingsClick,
                                statsClick = statsClick,
                                aboutClick = aboutClick,
                                helpClick = helpClick,
                                menuShowing = { visible -> mainDropdownShowing = visible },
                            ),
                        )
                )
            }
        },
        underHeaderActions = {
            AnimatedVisibility(
                !selectionMode && libraryScreenState.showLibraryButtonBar
            ) {
                LibraryButtonBar(
                    libraryScreenActions = libraryScreenActions,
                    libraryScreenState = libraryScreenState,
                    showCollapseAll =
                        libraryScreenState.items.size > 1 &&
                            !libraryScreenState.horizontalCategories,
                    groupByClick = {
                        scope.launch { openSheet(LibraryBottomSheetScreen.GroupBySheet) }
                    },
                )
            }
        },
        content = { incomingContentPadding ->*/
        val recyclerContentPadding = PaddingValues()
        /* PaddingValues(
            top = incomingContentPadding.calculateTopPadding(),
            bottom = incomingContentPadding.calculateBottomPadding(),
        )*/

        Box(modifier = Modifier.fillMaxSize()) {
            if (deleteMangaConfirmation) {
                ConfirmationDialog(
                    title = stringResource(R.string.remove),
                    body = stringResource(R.string.remove_from_library_question),
                    confirmButton = stringResource(R.string.remove),
                    onDismiss = { deleteMangaConfirmation = !deleteMangaConfirmation },
                    onConfirm = { libraryScreenActions.deleteSelectedLibraryMangaItems() },
                )
            }
            if (markActionConfirmation != null) {
                val (title, body) =
                    when (markActionConfirmation is ChapterMarkActions.Read) {
                        true -> R.string.mark_all_as_read to R.string.mark_all_chapters_as_read

                        false -> R.string.mark_all_as_unread to R.string.mark_all_chapters_as_unread
                    }
                ConfirmationDialog(
                    title = stringResource(title),
                    body = stringResource(body),
                    confirmButton = stringResource(R.string.mark),
                    onDismiss = { markActionConfirmation = null },
                    onConfirm = { libraryScreenActions.markMangaChapters(markActionConfirmation!!) },
                )
            }
            if (removeActionConfirmation != null) {
                val (title, body) =
                    when (removeActionConfirmation is MangaConstants.DownloadAction.RemoveAll) {
                        true -> R.string.remove_downloads to R.string.remove_all_downloads

                        false -> R.string.remove_downloads to R.string.remove_all_read_downloads
                    }
                ConfirmationDialog(
                    title = stringResource(title),
                    body = stringResource(body),
                    confirmButton = stringResource(R.string.remove),
                    onDismiss = { removeActionConfirmation = null },
                    onConfirm = {
                        libraryScreenActions.downloadChapters(removeActionConfirmation!!)
                    },
                )
            }

            if (libraryScreenState.items.isEmpty()) {

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (libraryScreenState.isFirstLoad) {
                        ContainedLoadingIndicator()
                    } else if (!libraryScreenState.searchQuery.isNullOrBlank()) {

                        EmptyScreen(
                            message = UiText.StringResource(resourceId = R.string.no_results_found),
                            actions =
                                persistentListOf(
                                    Action(
                                        text = (UiText.StringResource(R.string.search_globally)),
                                        onClick = {
                                            libraryScreenActions.searchMangaDex(
                                                libraryScreenState.searchQuery!!
                                            )
                                        },
                                    )
                                ),
                        )
                    } else {
                        EmptyScreen(
                            message =
                                UiText.StringResource(
                                    resourceId = R.string.library_is_empty_add_from_browse
                                )
                        )
                    }
                }
            } else {
                LibraryPage(
                    contentPadding = recyclerContentPadding,
                    libraryScreenState = libraryScreenState,
                    libraryScreenActions = libraryScreenActions,
                    libraryCategoryActions = libraryCategoryActions,
                    selectionMode = selectionMode,
                    categorySortClick = { categoryItem ->
                        scope.launch {
                            openSheet(
                                LibraryBottomSheetScreen.SortSheet(categoryItem = categoryItem)
                            )
                        }
                    },
                )
            }
        }

        // this is needed for Android SDK where blur isn't available
        if (mainDropdownShowing && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = NekoColors.mediumAlphaLowContrast))
            )
        }
    }
}
