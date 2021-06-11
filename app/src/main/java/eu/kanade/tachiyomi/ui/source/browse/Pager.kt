package eu.kanade.tachiyomi.ui.source.browse

import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.model.SManga
import rx.Observable

/**
 * A general pager for source requests (latest updates, popular, search)
 */
abstract class Pager(var currentPage: Int = 1) {

    var hasNextPage = true
        private set

    protected val results: PublishRelay<Pair<Int, List<SManga>>> = PublishRelay.create()

    fun results(): Observable<Pair<Int, List<SManga>>> {
        return results.asObservable()
    }

    abstract fun requestNext(): Observable<MangaListPage>

    fun onPageReceived(mangaListPage: MangaListPage) {
        val page = currentPage
        currentPage++
        hasNextPage = mangaListPage.hasNextPage && mangaListPage.manga.isNotEmpty()
        results.call(Pair(page, mangaListPage.manga))
    }
}
