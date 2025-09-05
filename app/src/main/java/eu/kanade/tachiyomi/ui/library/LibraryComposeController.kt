package eu.kanade.tachiyomi.ui.library

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.toLibraryManga
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.presentation.screens.LibraryScreen

class LibraryComposeController : BaseComposeController<LibraryComposePresenter>() {
    override val presenter = LibraryComposePresenter()

    @Composable
    override fun ScreenContent() {
        val windowSizeClass = calculateWindowSizeClass(this.activity!!)
        val isSideNav = (this.activity as? MainActivity)?.isSideNavigation() == true

        val context = LocalContext.current

        BackHandler((this.activity as? MainActivity)?.shouldGoToStartingTab() == true) {
            (this.activity as? MainActivity)?.backCallback?.invoke()
        }

        LibraryScreen(
            libraryScreenState = presenter.libraryScreenState.collectAsState(),
            libraryScreenActions =
                LibraryScreenActions(
                    mangaClick = ::openManga,
                    search = presenter::search,
                    updateLibrary = { start -> updateLibrary(start, context) },
                    collapseExpandAllCategories = presenter::collapseExpandAllCategories,
                ),
            librarySheetActions =
                LibrarySheetActions(
                    groupByClick = presenter::groupByClick,
                    categoryItemLibrarySortClick = presenter::categoryItemLibrarySortClick,
                    libraryDisplayModeClick = presenter::libraryDisplayModeClick,
                    rawColumnCountChanged = presenter::rawColumnCountChanged,
                    outlineCoversToggled = presenter::outlineCoversToggled,
                    downloadBadgesToggled = presenter::downloadBadgesToggled,
                    unreadBadgesToggled = presenter::unreadBadgesToggled,
                ),
            libraryCategoryActions =
                LibraryCategoryActions(
                    categoryItemClick = presenter::categoryItemClick,
                    categoryRefreshClick = { category -> updateCategory(category, context) },
                    dragAndDropManga = presenter::dragAndDropManga,
                ),
            windowSizeClass = windowSizeClass,
            incognitoClick = presenter::toggleIncognitoMode,
            legacySideNav = isSideNav,
            settingsClick = { (this.activity as? MainActivity)?.showSettings() },
            statsClick = { (this.activity as? MainActivity)?.showStats() },
            aboutClick = { (this.activity as? MainActivity)?.showAbout() },
            helpClick = {
                (this.activity as? MainActivity)?.openInBrowser("https://tachiyomi.org/help/")
            },
        )
    }

    private fun openManga(mangaId: Long) {
        viewScope.launchUI {
            router.pushController(MangaDetailController(mangaId).withFadeTransaction())
        }
    }

    private fun updateCategory(category: CategoryItem, context: Context) {
        if (!LibraryUpdateJob.categoryInQueue(category.id)) {
            LibraryUpdateJob.startNow(
                context = context,
                category.toDbCategory(),
                mangaToUse =
                    if (category.isDynamic) {
                        val libraryItems =
                            presenter.libraryScreenState.value.items
                                .firstOrNull { it.categoryItem.id == category.id }
                                ?.libraryItems
                                ?.map { it.toLibraryManga() }
                        libraryItems
                    } else {
                        null
                    },
            )
        }
    }

    private fun updateLibrary(start: Boolean, context: Context) {
        if (LibraryUpdateJob.isRunning(context) && !start) {
            presenter.refreshing(false)
            LibraryUpdateJob.stop(context)
        } else if (!LibraryUpdateJob.isRunning(context) && start) {
            presenter.refreshing(true)
            LibraryUpdateJob.startNow(context)
        }
    }
}
