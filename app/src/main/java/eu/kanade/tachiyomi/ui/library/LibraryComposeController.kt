package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.source.browse.BrowseController
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.toLibraryManga
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.R
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
                    mangaLongClick = presenter::libraryItemLongClick,
                    selectAllLibraryMangaItems = presenter::selectAllLibraryMangaItems,
                    deleteSelectedLibraryMangaItems = presenter::deleteSelectedLibraryMangaItems,
                    clearSelectedManga = presenter::clearSelectedManga,
                    search = presenter::search,
                    searchMangaDex = ::searchMangaDex,
                    updateLibrary = { updateLibrary(context) },
                    collapseExpandAllCategories = presenter::collapseExpandAllCategories,
                    clearActiveFilters = presenter::clearActiveFilters,
                    filterToggled = presenter::filterToggled,
                    downloadChapters = presenter::downloadChapters,
                    shareManga = { shareManga(context) },
                    mangaStartReadingClick = { mangaId ->
                        presenter.openNextUnread(
                            mangaId,
                            { manga, chapter ->
                                startActivity(ReaderActivity.newIntent(context, manga, chapter))
                            },
                        )
                    },
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
                    startReadingButtonToggled = presenter::startReadingButtonToggled,
                    horizontalCategoriesToggled = presenter::horizontalCategoriesToggled,
                    editCategories = presenter::editCategories,
                    addNewCategory = presenter::addNewCategory,
                ),
            libraryCategoryActions =
                LibraryCategoryActions(
                    categoryItemClick = presenter::categoryItemClick,
                    categoryAscendingClick = presenter::categoryAscendingClick,
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

    private fun searchMangaDex(query: String) {
        router.setRoot(
            BrowseController(query).withFadeTransaction().tag(R.id.nav_browse.toString())
        )
    }

    private fun shareManga(context: Context) {
        val urls = presenter.getSelectedMangaUrls()
        if (urls.isEmpty()) return
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/*"
                putExtra(Intent.EXTRA_TEXT, urls)
            }
        startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
    }

    private fun updateLibrary(context: Context) {
        if (!LibraryUpdateJob.isRunning(context)) {
            LibraryUpdateJob.startNow(context)
        }
    }
}
