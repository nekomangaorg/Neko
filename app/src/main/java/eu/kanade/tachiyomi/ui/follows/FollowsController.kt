package eu.kanade.tachiyomi.ui.follows

import android.os.Bundle
import android.view.Menu
import android.view.View
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourcePresenter
import eu.kanade.tachiyomi.util.view.gone
import kotlinx.android.synthetic.main.browse_source_controller.*

/**
 * Controller that shows the latest manga from the catalogue. Inherit [BrowseCatalogueController].
 */
class FollowsController(bundle: Bundle) : BrowseSourceController(bundle) {

    constructor() : this(Bundle().apply {
        putBoolean(APPLY_INSET, false)
        putBoolean(FOLLOWS, true)
    })

    override fun createPresenter(): BrowseSourcePresenter {
        return FollowsPresenter()
    }

    override fun getTitle(): String? {
        return view?.context?.getString(R.string.follows)
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

    override fun expandSearch() {
        activity?.onBackPressed()
    }
}
