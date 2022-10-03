package eu.kanade.tachiyomi.ui.follows

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.presentation.screens.FollowsScreen

/**
 * Controller that shows the follows for a logged in MangaDex user
 */
class FollowsController(bundle: Bundle? = null) : BaseComposeController<FollowsPresenter>() {
    override var presenter = FollowsPresenter()

    @Composable
    override fun ScreenContent() {
        FollowsScreen(
            followsScreenState = presenter.followsScreenState.collectAsState(),
            switchDisplayClick = presenter::switchDisplayMode,
            onBackPress = { activity?.onBackPressed() },
            mangaClick = { mangaId: Long -> router.pushController(MangaDetailController(mangaId).withFadeTransaction()) },
            addNewCategory = presenter::addNewCategory,
            toggleFavorite = presenter::toggleFavorite,
            retryClick = presenter::getFollows,
        )
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        presenter.updateCovers()
    }
}
