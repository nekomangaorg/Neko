package eu.kanade.tachiyomi.ui.source.latest

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.presentation.screens.LatestScreen

class LatestController(bundle: Bundle? = null) :
    BaseComposeController<LatestPresenter>(bundle) {

    override var presenter = LatestPresenter()

    @Composable
    override fun ScreenContent() {
        LatestScreen(
            latestScreenState = presenter.latestScreenState.collectAsState(),
            switchDisplayClick = presenter::switchDisplayMode,
            onBackPress = { activity?.onBackPressed() },
            openManga = { mangaId: Long -> router.pushController(MangaDetailController(mangaId).withFadeTransaction()) },
            addNewCategory = presenter::addNewCategory,
            toggleFavorite = presenter::toggleFavorite,
            loadNextPage = presenter::loadNextItems,
            retryClick = presenter::loadNextItems,
        )
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        presenter.updateCovers()
    }
}
