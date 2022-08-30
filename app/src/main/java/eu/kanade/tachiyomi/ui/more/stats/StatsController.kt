package eu.kanade.tachiyomi.ui.more.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailPresenter
import org.nekomanga.presentation.screens.StatsScreen

class StatsController : BaseComposeController<MangaDetailPresenter>() {

    override val presenter = StatsPresenter()

    @Composable
    override fun ScreenContent() {
        StatsScreen(
            statsState = presenter.statsState.collectAsState(),
            onBackPressed = { activity?.onBackPressed() },
        )
    }
}
