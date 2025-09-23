package org.nekomanga.presentation.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
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
import androidx.compose.ui.unit.dp
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.ui.library.LibraryCategoryActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenActions
import eu.kanade.tachiyomi.ui.library.LibraryScreenState
import eu.kanade.tachiyomi.ui.library.LibrarySheetActions
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.components.AppBarActions
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.components.PullRefresh
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dialog.ConfirmationDialog
import org.nekomanga.presentation.components.rememberNavBarPadding
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.screens.library.LibraryAppBarActions
import org.nekomanga.presentation.screens.library.LibraryBottomSheet
import org.nekomanga.presentation.screens.library.LibraryBottomSheetScreen
import org.nekomanga.presentation.screens.library.LibraryButtonBar
import org.nekomanga.presentation.screens.library.LibraryPage
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun LibraryScreen(
    libraryScreenState: State<LibraryScreenState>,
    libraryScreenActions: LibraryScreenActions,
    librarySheetActions: LibrarySheetActions,
    libraryCategoryActions: LibraryCategoryActions,
    windowSizeClass: WindowSizeClass,
    legacySideNav: Boolean,
    incognitoClick: () -> Unit,
    settingsClick: () -> Unit,
    statsClick: () -> Unit,
    helpClick: () -> Unit,
    aboutClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState =
        rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            skipHalfExpanded = true,
            animationSpec = tween(durationMillis = 150, easing = LinearEasing),
        )

    var mainDropdownShowing by remember { mutableStateOf(false) }

    var selectionMode by
        remember(libraryScreenState.value.selectedItems) {
            mutableStateOf(libraryScreenState.value.selectedItems.isNotEmpty())
        }

    var deleteMangaConfirmation by remember { mutableStateOf(false) }

    var currentBottomSheet: LibraryBottomSheetScreen? by remember { mutableStateOf(null) }

    /** Close the bottom sheet on back if its open */
    BackHandler(enabled = sheetState.isVisible) { scope.launch { sheetState.hide() } }

    // val sideNav = rememberSideBarVisible(windowSizeClass, feedScreenState.value.sideNavMode)
    val actualSideNav = legacySideNav
    val navBarPadding = rememberNavBarPadding(actualSideNav)

    // set the current sheet to null when bottom sheet is closed
    LaunchedEffect(key1 = sheetState.isVisible) {
        if (!sheetState.isVisible) {
            currentBottomSheet = null
        }
    }

    val openSheet: (LibraryBottomSheetScreen) -> Unit = {
        scope.launch {
            currentBottomSheet = it
            sheetState.show()
        }
    }

    Box(
        modifier =
            Modifier.fillMaxSize().conditional(mainDropdownShowing) {
                this.blur(Size.medium).clickable(enabled = false) {}
            }
    ) {
        ModalBottomSheetLayout(
            sheetState = sheetState,
            sheetShape =
                RoundedCornerShape(topStart = Shapes.sheetRadius, topEnd = Shapes.sheetRadius),
            sheetContent = {
                Box(modifier = Modifier.defaultMinSize(minHeight = Size.extraExtraTiny)) {
                    currentBottomSheet?.let { currentSheet ->
                        LibraryBottomSheet(
                            libraryScreenState = libraryScreenState.value,
                            librarySheetActions = librarySheetActions,
                            currentScreen = currentSheet,
                            contentPadding = navBarPadding,
                            closeSheet = { scope.launch { sheetState.hide() } },
                        )
                    }
                }
            },
        ) {
            PullRefresh(
                refreshing = libraryScreenState.value.isRefreshing,
                onRefresh = { libraryScreenActions.updateLibrary(true) },
                indicatorOffset =
                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                        WindowInsets.displayCutout.asPaddingValues().calculateTopPadding() +
                        Size.extraLarge,
            ) {
                NekoScaffold(
                    type =
                        if (selectionMode) NekoScaffoldType.TitleAndSubtitle
                        else NekoScaffoldType.SearchOutlineWithActions,
                    title =
                        if (selectionMode)
                            "Selected: ${libraryScreenState.value.selectedItems.size}"
                        else "",
                    searchPlaceHolder = stringResource(R.string.search_library),
                    searchPlaceHolderAlt = stringResource(R.string.library_search_hint),
                    incognitoMode = libraryScreenState.value.incognitoMode,
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
                                editCategoryClick = {
                                    scope.launch {
                                        openSheet(LibraryBottomSheetScreen.CategorySheet)
                                    }
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
                                            incognitoMode = libraryScreenState.value.incognitoMode,
                                            incognitoModeClick = incognitoClick,
                                            settingsClick = settingsClick,
                                            statsClick = statsClick,
                                            aboutClick = aboutClick,
                                            helpClick = helpClick,
                                            menuShowing = { visible ->
                                                mainDropdownShowing = visible
                                            },
                                        ),
                                    )
                            )
                        }
                    },
                    underHeaderActions = {
                        AnimatedVisibility(!selectionMode) {
                            LibraryButtonBar(
                                libraryScreenActions = libraryScreenActions,
                                libraryScreenState = libraryScreenState,
                                showCollapseAll = libraryScreenState.value.items.size > 1,
                                groupByClick = {
                                    scope.launch {
                                        openSheet(LibraryBottomSheetScreen.GroupBySheet)
                                    }
                                },
                            )
                        }
                    },
                    content = { incomingContentPadding ->
                        val recyclerContentPadding =
                            PaddingValues(
                                top = incomingContentPadding.calculateTopPadding(),
                                bottom =
                                    if (actualSideNav) {
                                        Size.navBarSize
                                    } else {
                                        Size.navBarSize
                                    } +
                                        WindowInsets.navigationBars
                                            .asPaddingValues()
                                            .calculateBottomPadding(),
                            )

                        Box(
                            modifier =
                                Modifier.padding(bottom = navBarPadding.calculateBottomPadding())
                                    .fillMaxSize()
                        ) {
                            if (deleteMangaConfirmation) {
                                ConfirmationDialog(
                                    title = stringResource(R.string.remove),
                                    body = stringResource(R.string.remove_from_library_question),
                                    confirmButton = stringResource(R.string.remove),
                                    onDismiss = {
                                        deleteMangaConfirmation = !deleteMangaConfirmation
                                    },
                                    onConfirm = {
                                        libraryScreenActions.deleteSelectedLibraryMangaItems()
                                    },
                                )
                            }

                            if (libraryScreenState.value.items.isEmpty()) {

                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (libraryScreenState.value.isFirstLoad) {
                                        CircularWavyProgressIndicator()
                                    } else {
                                        EmptyScreen(
                                            iconicImage = CommunityMaterial.Icon2.cmd_heart_off,
                                            iconSize = 176.dp,
                                            message =
                                                stringResource(
                                                    id = R.string.library_is_empty_add_from_browse
                                                ),
                                        )
                                    }
                                }
                            } else {
                                LibraryPage(
                                    contentPadding = recyclerContentPadding,
                                    libraryScreenState = libraryScreenState.value,
                                    libraryScreenActions = libraryScreenActions,
                                    libraryCategoryActions = libraryCategoryActions,
                                    selectionMode = selectionMode,
                                    categorySortClick = { categoryItem ->
                                        scope.launch {
                                            openSheet(
                                                LibraryBottomSheetScreen.SortSheet(
                                                    categoryItem = categoryItem
                                                )
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    },
                )

                // this is needed for Android SDK where blur isn't available
                if (mainDropdownShowing && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(
                                    Color.Black.copy(alpha = NekoColors.mediumAlphaLowContrast)
                                )
                    )
                }
            }
        }
    }
}
