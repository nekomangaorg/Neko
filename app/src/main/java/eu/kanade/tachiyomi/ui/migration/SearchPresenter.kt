package eu.kanade.tachiyomi.ui.migration

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.source.global_search.SourceSearchCardItem
import eu.kanade.tachiyomi.ui.source.global_search.SourceSearchItem
import eu.kanade.tachiyomi.ui.source.global_search.SourceSearchPresenter

class SearchPresenter(
    initialQuery: String? = "",
    private val manga: Manga
) : SourceSearchPresenter(initialQuery) {

    override fun getEnabledSources(): List<CatalogueSource> {
        // Put the source of the selected manga at the top
        return super.getEnabledSources()
                .sortedByDescending { it.id == manga.source }
    }

    override fun createCatalogueSearchItem(source: CatalogueSource, results: List<SourceSearchCardItem>?): SourceSearchItem {
        // Set the catalogue search item as highlighted if the source matches that of the selected manga
        return SourceSearchItem(source, results, source.id == manga.source)
    }
}
