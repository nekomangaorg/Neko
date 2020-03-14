package eu.kanade.tachiyomi.ui.manga.similar

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.catalogue.browse.BrowseCataloguePresenter
import eu.kanade.tachiyomi.ui.catalogue.browse.Pager

/**
 * Presenter of [SimilarController]. Inherit BrowseCataloguePresenter.
 */
class SimilarPresenter(sourceId: Long) : BrowseCataloguePresenter(sourceId) {

    var manga: Manga? = null
        private set

    constructor(manga: Manga, sourceId: Long) : this(sourceId) {
        this.manga = manga
    }

    override fun createPager(query: String, filters: FilterList): Pager {
        return SimilarPager(this.manga!!, source)
    }
}
