package eu.kanade.tachiyomi.ui.similar

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.presentation.screens.SimilarScreen

/**
 * Controller that shows the similar/related manga
 */
class SimilarController(mangaUUID: String) : BaseComposeController<SimilarPresenter>() {

    constructor(bundle: Bundle) : this(bundle.getString(MANGA_EXTRA) ?: "")

    override var presenter = SimilarPresenter(mangaUUID)

    @Composable
    override fun ScreenContent() {
        SimilarScreen(
            similarScreenState = presenter.similarScreenState.collectAsState(),
            switchDisplayClick = presenter::switchDisplayMode,
            onBackPress = { activity?.onBackPressed() },
            mangaClick = { mangaId: Long -> router.pushController(MangaDetailController(mangaId).withFadeTransaction()) },
            addNewCategory = presenter::addNewCategory,
            toggleFavorite = presenter::toggleFavorite,
            onRefresh = presenter::refresh,
        )
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        presenter.updateCovers()
    }

    companion object {
        const val MANGA_EXTRA = "manga"
    }
}
