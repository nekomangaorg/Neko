package eu.kanade.tachiyomi.ui.source

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourcePresenter

class LatestSourceController(bundle: Bundle) : BrowseSourceController(bundle) {
    constructor() : this(
        Bundle().apply {
            putBoolean(APPLY_INSET, false)
        }
    )

    override fun getTitle(): String? {
        return view?.context?.getString(R.string.latest)
    }

    override fun createPresenter(): BrowseSourcePresenter {
        return LatestSourcePresenter().apply {
            shouldHideFab = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    }
}
