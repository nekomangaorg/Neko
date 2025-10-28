package org.nekomanga.presentation.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.library.LibraryCategoryActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import eu.kanade.tachiyomi.ui.library.LibrarySheetActions
import eu.kanade.tachiyomi.ui.library.LibraryViewModel
import eu.kanade.tachiyomi.ui.main.states.LocalBarUpdater
import eu.kanade.tachiyomi.ui.main.states.LocalPullRefreshState
import eu.kanade.tachiyomi.ui.main.states.PullRefreshState
import eu.kanade.tachiyomi.ui.main.states.ScreenBars
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.toLibraryManga
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dialog.ConfirmationDialog
import org.nekomanga.presentation.screens.library.HorizontalCategoriesPage
import org.nekomanga.presentation.screens.library.LibraryBottomSheet
import org.nekomanga.presentation.screens.library.LibraryBottomSheetScreen
import org.nekomanga.presentation.screens.library.LibraryScreenTopBar
import org.nekomanga.presentation.screens.library.VerticalCategoriesPage
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel,
    mainDropDown: AppBar.MainDropdown,
    openManga: (Long) -> Unit,
    searchMangaDex: (String) -> Unit,
    windowSizeClass: WindowSizeClass,
) {
    val context = LocalContext.current
    LibraryWrapper(
        libraryStateFlow = libraryViewModel.libraryScreenState,
        mainDropDown = mainDropDown,
        libraryScreenActions =
            LibraryScreenActions(
                mangaClick = openManga,
                mangaLongClick = libraryViewModel::libraryItemLongClick,
                selectAllLibraryMangaItems = libraryViewModel::selectAllLibraryMangaItems,
                deleteSelectedLibraryMangaItems = libraryViewModel::deleteSelectedLibraryMangaItems,
                clearSelectedManga = libraryViewModel::clearSelectedManga,
                search = libraryViewModel::search,
                searchMangaDex = searchMangaDex,
                updateLibrary = {
                    if (!LibraryUpdateJob.isRunning(context)) {
                        LibraryUpdateJob.startNow(context)
                    }
                },
                collapseExpandAllCategories = libraryViewModel::collapseExpandAllCategories,
                clearActiveFilters = libraryViewModel::clearActiveFilters,
                filterToggled = libraryViewModel::filterToggled,
                downloadChapters = libraryViewModel::downloadChapters,
                shareManga = {
                    val urls = libraryViewModel.getSelectedMangaUrls()
                    if (urls.isEmpty()) return@LibraryScreenActions
                    val intent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/*"
                            putExtra(Intent.EXTRA_TEXT, urls)
                        }
                    context.startActivity(
                        Intent.createChooser(intent, context.getString(R.string.share))
                    )
                },
                markMangaChapters = libraryViewModel::markChapters,
                syncMangaToDex = libraryViewModel::syncMangaToDex,
                mangaStartReadingClick = { mangaId ->
                    libraryViewModel.openNextUnread(
                        mangaId,
                        { manga, chapter ->
                            context.startActivity(ReaderActivity.newIntent(context, manga, chapter))
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
                categoryRefreshClick = { categoryItem ->
                    LibraryUpdateJob.startNow(
                        context = context,
                        categoryItem.toDbCategory(),
                        mangaToUse =
                            if (categoryItem.isDynamic) {
                                val libraryItems =
                                    libraryViewModel.libraryScreenState.value.items
                                        .firstOrNull { it.categoryItem.id == categoryItem.id }
                                        ?.libraryItems
                                        ?.map { it.toLibraryManga() }
                                libraryItems
                            } else {
                                null
                            },
                    )
                },
            ),
        windowSizeClass = windowSizeClass,
    )
}

@Composable
private fun LibraryWrapper(
    libraryStateFlow: StateFlow<LibraryScreenState>,
    mainDropDown: AppBar.MainDropdown,
    libraryScreenActions: LibraryScreenActions,
    librarySheetActions: LibrarySheetActions,
    libraryCategoryActions: LibraryCategoryActions,
    windowSizeClass: WindowSizeClass,
) {

    val libraryScreenState by libraryStateFlow.collectAsState()

    val updateTopBar = LocalBarUpdater.current
    val updateRefreshState = LocalPullRefreshState.current

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pullRefreshState = remember { PullRefreshState() }

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

    Box(modifier = Modifier.fillMaxSize()) {
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

        val screenBars = remember {
            ScreenBars(
                topBar = {
                    LibraryScreenTopBar(
                        scrollBehavior = scrollBehavior,
                        mainDropDown = mainDropDown,
                        libraryScreenState = libraryScreenState,
                        libraryScreenActions = libraryScreenActions,
                        displayOptionsClick = {
                            scope.launch { openSheet(LibraryBottomSheetScreen.DisplayOptionsSheet) }
                        },
                        groupByClick = {
                            scope.launch { openSheet(LibraryBottomSheetScreen.GroupBySheet) }
                        },
                        editCategoryClick = {
                            scope.launch { openSheet(LibraryBottomSheetScreen.CategorySheet) }
                        },
                        removeFromLibraryClick = { deleteMangaConfirmation = true },
                        markActionClick = { markAction -> markActionConfirmation = markAction },
                        removeActionClick = { removeAction ->
                            removeActionConfirmation = removeAction
                        },
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        }
        DisposableEffect(Unit) {
            updateTopBar(screenBars)
            onDispose { updateTopBar(ScreenBars(id = screenBars.id, topBar = null)) }
        }

        DisposableEffect(libraryScreenState.isRefreshing, libraryScreenActions.updateLibrary) {
            updateRefreshState(
                pullRefreshState.copy(
                    enabled = true,
                    isRefreshing = libraryScreenState.isRefreshing,
                    onRefresh = libraryScreenActions.updateLibrary,
                )
            )
            onDispose { updateRefreshState(pullRefreshState.copy(onRefresh = null)) }
        }

        val recyclerContentPadding = PaddingValues()

        Box(modifier = Modifier.fillMaxSize()) {
            LibraryDialogs(
                deleteMangaConfirmation = deleteMangaConfirmation,
                markActionConfirmation = markActionConfirmation,
                removeActionConfirmation = removeActionConfirmation,
                onDeleteDismiss = { deleteMangaConfirmation = !deleteMangaConfirmation },
                onDeleteConfirm = { libraryScreenActions.deleteSelectedLibraryMangaItems() },
                onMarkDismiss = { markActionConfirmation = null },
                onMarkConfirm = {
                    libraryScreenActions.markMangaChapters(markActionConfirmation!!)
                },
                onRemoveDismiss = { removeActionConfirmation = null },
                onRemoveConfirm = {
                    libraryScreenActions.downloadChapters(removeActionConfirmation!!)
                },
            )

            if (libraryScreenState.items.isEmpty()) {
                EmptyLibrary(
                    libraryScreenState = libraryScreenState,
                    searchMangaDex = libraryScreenActions.searchMangaDex,
                )
            } else {
                if (libraryScreenState.horizontalCategories) {
                    HorizontalCategoriesPage(
                        contentPadding = recyclerContentPadding,
                        selectionMode = selectionMode,
                        libraryScreenState = libraryScreenState,
                        libraryScreenActions = libraryScreenActions,
                        libraryCategoryActions = libraryCategoryActions,
                        categorySortClick = { categoryItem ->
                            scope.launch {
                                openSheet(
                                    LibraryBottomSheetScreen.SortSheet(categoryItem = categoryItem)
                                )
                            }
                        },
                    )
                } else {
                    VerticalCategoriesPage(
                        contentPadding = recyclerContentPadding,
                        selectionMode = selectionMode,
                        libraryScreenState = libraryScreenState,
                        libraryScreenActions = libraryScreenActions,
                        libraryCategoryActions = libraryCategoryActions,
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
        }
    }
}

@Composable
private fun EmptyLibrary(
    libraryScreenState: eu.kanade.tachiyomi.ui.library.LibraryScreenState,
    searchMangaDex: (String) -> Unit,
) {
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
                            onClick = { searchMangaDex(libraryScreenState.searchQuery!!) },
                        )
                    ),
            )
        } else {
            EmptyScreen(
                message =
                    UiText.StringResource(resourceId = R.string.library_is_empty_add_from_browse)
            )
        }
    }
}

@Composable
private fun LibraryDialogs(
    deleteMangaConfirmation: Boolean,
    markActionConfirmation: ChapterMarkActions?,
    removeActionConfirmation: MangaConstants.DownloadAction?,
    onDeleteDismiss: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onMarkDismiss: () -> Unit,
    onMarkConfirm: () -> Unit,
    onRemoveDismiss: () -> Unit,
    onRemoveConfirm: () -> Unit,
) {
    if (deleteMangaConfirmation) {
        ConfirmationDialog(
            title = stringResource(R.string.remove),
            body = stringResource(R.string.remove_from_library_question),
            confirmButton = stringResource(R.string.remove),
            onDismiss = onDeleteDismiss,
            onConfirm = onDeleteConfirm,
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
            onDismiss = onMarkDismiss,
            onConfirm = onMarkConfirm,
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
            onDismiss = onRemoveDismiss,
            onConfirm = onRemoveConfirm,
        )
    }
}
