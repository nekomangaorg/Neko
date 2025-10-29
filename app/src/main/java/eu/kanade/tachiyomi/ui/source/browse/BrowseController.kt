package eu.kanade.tachiyomi.ui.source.browse

import android.view.View
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.view.withFadeTransaction

class BrowseController(incomingQuery: String = "") : BaseComposeController<BrowsePresenter>() {
    override val presenter = BrowsePresenter(incomingQuery)

    @Composable override fun ScreenContent() {}

    fun searchByTag(tag: String) {
        presenter.searchTag(tag)
    }

    fun searchByCreator(creator: String) {
        presenter.searchCreator(creator)
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
