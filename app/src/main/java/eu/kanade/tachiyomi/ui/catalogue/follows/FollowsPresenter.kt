package eu.kanade.tachiyomi.ui.catalogue.follows

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.catalogue.browse.BrowseCataloguePresenter
import eu.kanade.tachiyomi.ui.catalogue.browse.Pager

/**
 * Presenter of [FollowsController]. Inherit BrowseCataloguePresenter.
 */
class FollowsPresenter(sourceId: Long) : BrowseCataloguePresenter(sourceId) {

    override fun createPager(query: String, filters: FilterList): Pager {
        return FollowsPager(source)
    }

}