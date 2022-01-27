package eu.kanade.tachiyomi.ui.follows

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourcePresenter
import eu.kanade.tachiyomi.ui.source.browse.Pager

/**
 * Presenter of [SimilarController]. Inherit BrowseSourcePresenter.
 */
class FollowsPresenter : BrowseSourcePresenter() {

    override fun createPager(query: String, filters: FilterList): Pager {
        isFollows = true
        return FollowsPager(source)
    }
}
