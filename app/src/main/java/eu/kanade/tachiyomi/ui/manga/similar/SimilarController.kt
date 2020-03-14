package eu.kanade.tachiyomi.ui.manga.similar

import android.os.Bundle
import android.view.Menu
import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.catalogue.browse.BrowseCatalogueController
import eu.kanade.tachiyomi.ui.catalogue.browse.BrowseCataloguePresenter

/**
 * Controller that shows the latest manga from the catalogue. Inherit [BrowseCatalogueController].
 */
class SimilarController(bundle: Bundle) : BrowseCatalogueController(bundle) {

    var manga: Manga? = null
        private set

    constructor(manga: Manga, source: Source) : this(Bundle().apply {
        putLong(SOURCE_ID_KEY, source.id)
    }) {
        this.manga = manga
    }

    override fun createPresenter(): BrowseCataloguePresenter {
        return SimilarPresenter(this.manga!!, args.getLong(SOURCE_ID_KEY))
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_search).isVisible = false
        menu.findItem(R.id.action_set_filter).isVisible = false
        menu.findItem(R.id.action_open_in_web_view).isVisible = false
    }

    override fun createSecondaryDrawer(drawer: DrawerLayout): ViewGroup? {
        return null
    }

    override fun cleanupSecondaryDrawer(drawer: DrawerLayout) {
    }
}
