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
        val isSideNav = (this.activity as? MainActivity)?.isSideNavigation() == true

        BackHandler((this.activity as? MainActivity)?.shouldGoToStartingTab() == true) {
            (this.activity as? MainActivity)?.backCallback?.invoke()
        }

        BrowseScreen(
            browseScreenState = presenter.browseScreenState.collectAsState(),
            onBackPress = router::handleBack,
            windowSizeClass = windowSizeClass,
            legacySideNav = isSideNav,
            homeScreenTitleClick = ::openDisplayScreen,
            openManga = ::openManga,
            filterActions =
                FilterActions(
                    filterClick = { presenter.handle(BrowseAction.SetScreenType(BrowseScreenType.Filter)) },
                    filterChanged = { presenter.handle(BrowseAction.FilterChanged(it)) },
                    resetClick = { presenter.handle(BrowseAction.ResetFilter) },
                    saveFilterClick = { presenter.handle(BrowseAction.SaveFilter(it)) },
                    deleteFilterClick = { presenter.handle(BrowseAction.DeleteFilter(it)) },
                    filterDefaultClick = { name, makeDefault ->
                        presenter.handle(BrowseAction.MarkFilterAsDefault(name, makeDefault))
                    },
                    loadFilter = { presenter.handle(BrowseAction.LoadFilter(it)) },
                ),
            browseActions = BrowseActions(
                switchDisplayClick = { presenter.handle(BrowseAction.SwitchDisplayMode) },
                switchLibraryVisibilityClick = { presenter.handle(BrowseAction.SwitchLibraryVisibility) },
                addNewCategory = { presenter.handle(BrowseAction.AddNewCategory(it)) },
                toggleFavorite = { mangaId, categories ->
                    presenter.handle(BrowseAction.ToggleFavorite(mangaId, categories))
                },
                loadNextPage = { presenter.handle(BrowseAction.LoadNextPage) },
                retryClick = { presenter.handle(BrowseAction.Retry) },
                otherClick = { presenter.handle(BrowseAction.OtherClick(it)) },
                changeScreenType = { screenType, force ->
                    presenter.handle(BrowseAction.SetScreenType(screenType, force))
                },
                randomClick = { presenter.handle(BrowseAction.RandomManga) },
                incognitoClick = { presenter.handle(BrowseAction.ToggleIncognito) },
                settingsClick = { (this.activity as? MainActivity)?.showSettings() },
                statsClick = { (this.activity as? MainActivity)?.showStats() },
                aboutClick = { (this.activity as? MainActivity)?.showAbout() },
                helpClick = {
                    (this.activity as? MainActivity)?.openInBrowser(
                        "https://tachiyomi.org/docs/guides/troubleshooting/"
                    )
                },
            )
        )
    }

    fun searchByTag(tag: String) {
        presenter.handle(BrowseAction.TagSearch(tag))
    }

    fun searchByCreator(creator: String) {
        presenter.handle(BrowseAction.CreatorSearch(creator))
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
