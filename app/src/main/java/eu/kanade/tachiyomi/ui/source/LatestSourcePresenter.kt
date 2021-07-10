package eu.kanade.tachiyomi.ui.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourcePresenter
import eu.kanade.tachiyomi.ui.source.browse.Pager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class LatestSourcePresenter : BrowseSourcePresenter() {
    var scope = CoroutineScope(Job() + Dispatchers.Default)

    override fun createPager(query: String, filters: FilterList): Pager {
        return LatestSourcePager(scope, source)
    }
}
