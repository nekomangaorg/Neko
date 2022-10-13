package eu.kanade.tachiyomi.ui.source.browse

import android.view.View
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.ui.source.latest.DisplayController
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.view.requestFilePermissionsSafe
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.presentation.screens.BrowseScreen

class BrowseComposeController(incomingQuery: String = "") : BaseComposeController<BrowseComposePresenter>() {
    override val presenter = BrowseComposePresenter(incomingQuery)

    @Composable
    override fun ScreenContent() {
        val windowSizeClass = calculateWindowSizeClass(this.activity!!)

        BrowseScreen(
            browseScreenState = presenter.browseScreenState.collectAsState(),
            switchDisplayClick = presenter::switchDisplayMode,
            switchLibraryVisibilityClick = presenter::switchLibraryVisibility,
            onBackPress = { activity?.onBackPressed() },
            windowSizeClass = windowSizeClass,
            homeScreenTitleClick = ::openDisplayScreen,
            openManga = ::openManga,
            filterActions = FilterActions(
                filterClick = presenter::getSearchPage,
                filterChanged = presenter::filterChanged,
                resetClick = presenter::resetFilter,
                saveClick = presenter::saveFilter,
            ),
            addNewCategory = presenter::addNewCategory,
            toggleFavorite = presenter::toggleFavorite,
            loadNextPage = presenter::loadNextItems,
            retryClick = presenter::loadNextItems,
            otherClick = presenter::otherClick,
            changeScreenType = presenter::changeScreenType,
        )
    }

    fun openDisplayScreen(displayScreenType: DisplayScreenType) {
        viewScope.launchUI {
            router.pushController(DisplayController(displayScreenType).withFadeTransaction())
        }
    }

    fun openManga(mangaId: Long) {
        viewScope.launchUI {
            router.pushController(MangaDetailController(mangaId).withFadeTransaction())
        }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        requestFilePermissionsSafe(301, presenter.preferences)
        presenter.updateMangaForChanges()
    }
}
