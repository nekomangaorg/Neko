package eu.kanade.tachiyomi.ui.source.browse

import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.ui.source.latest.DisplayController
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.presentation.screens.BrowseScreen

class BrowseComposeController(query: String? = null) : BaseComposeController<BrowseComposePresenter>() {
    override val presenter = BrowseComposePresenter()

    @Composable
    override fun ScreenContent() {
        val windowSizeClass = calculateWindowSizeClass(this.activity!!)

        BrowseScreen(
            browseScreenState = presenter.browseScreenState.collectAsState(),
            switchDisplayClick = presenter::switchDisplayMode,
            switchLibraryVisibilityClick = presenter::switchLibraryVisibility,
            onBackPress = { activity?.onBackPressed() },
            windowSizeClass = windowSizeClass,
            homeScreenTitleClick = { displayScreenType, name -> router.pushController(DisplayController(displayScreenType, name).withFadeTransaction()) },
            openManga = { mangaId: Long -> router.pushController(MangaDetailController(mangaId).withFadeTransaction()) },
            addNewCategory = presenter::addNewCategory,
            toggleFavorite = presenter::toggleFavorite,
            loadNextPage = presenter::loadNextItems,
            retryClick = presenter::loadNextItems,
            changeScreenType = presenter::changeScreenType,
        )
    }
}
