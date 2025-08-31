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
import eu.kanade.tachiyomi.util.system.openInBrowser
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
                    search = presenter::search,
                    updateLibrary = { start -> updateLibrary(start, context) },
                ),
            libraryCategoryActions =
                LibraryCategoryActions(
                    categoryItemClick = presenter::categoryItemClick,
                    categoryRefreshClick = {},
                    categoryItemLibrarySortClick = presenter::categoryItemLibrarySortClick,
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
