package eu.kanade.tachiyomi.ui.manga.similar

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourcePresenter
import eu.kanade.tachiyomi.ui.source.browse.Pager

/**
 * Presenter of [SimilarController]. Inherit BrowseSourcePresenter.
 */
class SimilarPresenter(val mangaId: Long) : BrowseSourcePresenter() {

    var manga: Manga? = null
    
    override fun createPager(query: String, filters: FilterList): Pager {
        this.manga = db.getManga(mangaId).executeAsBlocking()
        return SimilarPager(this.manga!!, source)
    }
}
