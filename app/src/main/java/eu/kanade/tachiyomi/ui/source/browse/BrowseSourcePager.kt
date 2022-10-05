package eu.kanade.tachiyomi.ui.source.browse

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.MangaDex

open class BrowseSourcePager(
    val source: MangaDex,
    val query: String,
    val filters: FilterList,
) :
    Pager() {

    override suspend fun requestNextPage() {
        val page = currentPage

        val mangaListPage = source.search(page, query, filters)

        if (mangaListPage.manga.isNotEmpty()) {
            onPageReceived(mangaListPage)
        } else {
            throw NoResultsException()
        }
    }
}
