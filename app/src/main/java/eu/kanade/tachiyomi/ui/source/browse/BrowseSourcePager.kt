package eu.kanade.tachiyomi.ui.source.browse

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.util.system.runAsObservable
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

open class BrowseSourcePager(val source: Source, val query: String, val filters: FilterList) :
    Pager() {

    override fun requestNext(): Observable<MangaListPage> {
        val page = currentPage
        return runAsObservable {
            (source as MangaDex).search(page, query, filters)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                if (it.manga.isNotEmpty()) {
                    onPageReceived(it)
                } else {
                    throw NoResultsException()
                }
            }
    }
}
