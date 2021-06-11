package eu.kanade.tachiyomi.ui.similar

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.manga.similar.SimilarPresenter
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourcePresenter
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.setStyle
import eu.kanade.tachiyomi.util.view.snack

/**
 * Controller that shows the latest manga from the catalogue. Inherit [BrowseCatalogueController].
 */
class SimilarController(bundle: Bundle) : BrowseSourceController(bundle) {

    lateinit var similarPresenter: SimilarPresenter

    constructor(manga: Manga, source: Source) : this(
        Bundle().apply {
            putLong(MANGA_ID, manga.id!!)
            putLong(SOURCE_ID_KEY, source.id)
            putBoolean(APPLY_INSET, false)
        }
    )

    override fun getTitle(): String? {
        return view?.context?.getString(R.string.similar)
    }

    override fun createPresenter(): BrowseSourcePresenter {
        similarPresenter = SimilarPresenter(bundle!!.getLong(MANGA_ID), this)
        return similarPresenter
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        binding.fab.isVisible = false
        binding.swipeRefresh.setStyle()

        binding.swipeRefresh.setProgressViewOffset(false, 20.dpToPx, binding.swipeRefresh.progressViewEndOffset + 25.dpToPx)
        binding.swipeRefresh.isEnabled = true
        binding.swipeRefresh.setOnRefreshListener {
            similarPresenter.refreshSimilarManga()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    }

    fun showUserMessage(message: String) {
        binding.swipeRefresh.isRefreshing = false
        view?.snack(message, Snackbar.LENGTH_LONG)
    }

    /**
     * Called from the presenter when the network request fails.
     *
     * @param error the error received.
     */
    override fun onAddPageError(error: Throwable) {
        super.onAddPageError(error)
        binding.emptyView.show(
            CommunityMaterial.Icon.cmd_compass_off,
            "No Similar Manga found"
        )
    }
}
