package eu.kanade.tachiyomi.ui.source

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourcePresenter

class LatestSourceController(bundle: Bundle) : BrowseSourceController(bundle) {
    constructor() : this(
        Bundle().apply {
            putBoolean(APPLY_INSET, false)
        }
    )

    override fun createPresenter(): BrowseSourcePresenter {
        return LatestSourcePresenter()
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        binding.fab.isVisible = false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    }
}
