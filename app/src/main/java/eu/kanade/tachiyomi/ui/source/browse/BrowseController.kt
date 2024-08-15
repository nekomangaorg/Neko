package eu.kanade.tachiyomi.ui.source.browse

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.ui.source.latest.DisplayController
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.presentation.screens.BrowseScreen

class BrowseController(incomingQuery: String = "") : BaseComposeController<BrowsePresenter>() {
    override val presenter = BrowsePresenter(incomingQuery)

    @Composable
    override fun ScreenContent() {
        val windowSizeClass = calculateWindowSizeClass(this.activity!!)
        val isSideNav = (this.activity as? MainActivity)?.isSideNavigation() ?: false

        BackHandler((this.activity as? MainActivity)?.shouldGoToStartingTab() == true) {
            (this.activity as? MainActivity)?.backCallback?.invoke()
        }

        BrowseScreen(
            browseScreenState = presenter.browseScreenState.collectAsState(),
            switchDisplayClick = presenter::switchDisplayMode,
            switchLibraryVisibilityClick = presenter::switchLibraryVisibility,
            onBackPress = router::handleBack,
            windowSizeClass = windowSizeClass,
            legacySideNav = isSideNav,
            homeScreenTitleClick = ::openDisplayScreen,
            openManga = ::openManga,
            filterActions =
                FilterActions(
                    filterClick = presenter::getSearchPage,
                    filterChanged = presenter::filterChanged,
                    resetClick = presenter::resetFilter,
                    saveFilterClick = presenter::saveFilter,
                    deleteFilterClick = presenter::deleteFilter,
                    filterDefaultClick = presenter::markFilterAsDefault,
                    loadFilter = presenter::loadFilter,
                ),
            addNewCategory = presenter::addNewCategory,
            toggleFavorite = presenter::toggleFavorite,
            loadNextPage = presenter::loadNextItems,
            retryClick = presenter::retry,
            otherClick = presenter::otherClick,
            listClick = { title: String, uuid: String -> openDisplayScreen(DisplayScreenType.List(title, uuid, true)) },
            changeScreenType = presenter::changeScreenType,
            randomClick = presenter::randomManga,
            incognitoClick = presenter::toggleIncognitoMode,
            settingsClick = { (this.activity as? MainActivity)?.showSettings() },
            statsClick = { (this.activity as? MainActivity)?.showStats() },
            aboutClick = { (this.activity as? MainActivity)?.showAbout() },
            helpClick = {
                (this.activity as? MainActivity)?.openInBrowser(
                    "https://tachiyomi.org/docs/guides/troubleshooting/")
            },
        )
    }

    fun searchByTag(tag: String) {
        presenter.searchTag(tag)
    }

    fun searchByCreator(creator: String) {
        presenter.searchCreator(creator)
    }

    private fun openDisplayScreen(displayScreenType: DisplayScreenType) {
        viewScope.launchUI {
            router.pushController(DisplayController(displayScreenType).withFadeTransaction())
        }
    }

    fun openManga(mangaId: Long, wasDeepLink: Boolean = false) {
        viewScope.launchUI {
            if (wasDeepLink) {
                router.replaceTopController(MangaDetailController(mangaId).withFadeTransaction())
            } else {
                router.pushController(MangaDetailController(mangaId).withFadeTransaction())
            }
        }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        presenter.updateMangaForChanges()
    }
}
