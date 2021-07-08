package eu.kanade.tachiyomi.ui.source

import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.ui.source.browse.NoResultsException
import eu.kanade.tachiyomi.ui.source.browse.Pager
import eu.kanade.tachiyomi.util.system.runAsObservable
import kotlinx.coroutines.CoroutineScope
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * LatestUpdatesPager inherited from the general Pager.
 */
class LatestSourcePager(val scope: CoroutineScope, val source: MangaDex) : Pager() {

    override fun requestNext(): Observable<MangaListPage> {
        val page = currentPage
        return runAsObservable(scope) {
            source.latestChapters(page)
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
