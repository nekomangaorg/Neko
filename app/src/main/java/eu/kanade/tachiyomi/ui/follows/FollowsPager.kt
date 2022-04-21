package eu.kanade.tachiyomi.ui.follows

import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.ui.source.browse.NoResultsException
import eu.kanade.tachiyomi.ui.source.browse.Pager

/**
 * LatestUpdatesPager inherited from the general Pager.
 */
class FollowsPager(val source: MangaDex) : Pager() {

    override suspend fun requestNextPage() {

        val mangaListPage = source.fetchFollowList()

        if (mangaListPage.manga.isNotEmpty()) {
            onPageReceived(mangaListPage)
        } else {
            throw NoResultsException()
        }
    }
}
