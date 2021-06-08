package eu.kanade.tachiyomi.ui.follows

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourcePresenter

/**
 * Controller that shows the latest manga from the catalogue. Inherit [BrowseCatalogueController].
 */
class FollowsController(bundle: Bundle) : BrowseSourceController(bundle) {

    constructor() : this(
        Bundle().apply {
            putBoolean(APPLY_INSET, false)
            putBoolean(FOLLOWS, true)
        }
    )

    override fun createPresenter(): BrowseSourcePresenter {
        return FollowsPresenter()
    }

    override fun getTitle(): String? {
        return view?.context?.getString(R.string.follows)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        binding.fab.isVisible = false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    }
}
