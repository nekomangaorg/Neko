package eu.kanade.tachiyomi.ui.similar

import android.os.Bundle
import android.view.Menu
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.manga.similar.SimilarPresenter
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourcePresenter
import eu.kanade.tachiyomi.util.view.gone
import kotlinx.android.synthetic.main.browse_source_controller.*

/**
 * Controller that shows the latest manga from the catalogue. Inherit [BrowseCatalogueController].
 */
class SimilarController(bundle: Bundle) : BrowseSourceController(bundle) {

    var manga: Manga? = null
        private set

    constructor(manga: Manga, source: Source) : this(Bundle().apply {
        putLong(SOURCE_ID_KEY, source.id)
        putBoolean(APPLY_INSET, false)
    }) {
        this.manga = manga
    }

    override fun createPresenter(): BrowseSourcePresenter {
        return SimilarPresenter(this.manga!!, args.getLong(SOURCE_ID_KEY))
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        fab.gone()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_search).isVisible = false
        menu.findItem(R.id.action_open_in_web_view).isVisible = false
    }
}
