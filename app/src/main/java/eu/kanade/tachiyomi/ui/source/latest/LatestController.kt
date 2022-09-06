package eu.kanade.tachiyomi.ui.source.latest

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.util.view.numberOfColumnsForCompose
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.presentation.screens.LatestScreen

class LatestController(bundle: Bundle? = null) :
    BaseComposeController<LatestPresenter>(bundle) {

    override var presenter = LatestPresenter()

    private var columns: Int = 0

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        columns = binding.root.rootView.measuredWidth.numberOfColumnsForCompose(presenter.preferences.gridSize().get())
    }

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
        )
    }
}
